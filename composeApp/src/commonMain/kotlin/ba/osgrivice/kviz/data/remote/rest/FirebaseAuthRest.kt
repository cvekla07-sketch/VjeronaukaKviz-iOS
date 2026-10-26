package ba.osgrivice.kviz.data.remote.rest

import ba.osgrivice.kviz.platform.LokalniKljucevi
import ba.osgrivice.kviz.platform.sadaMs
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Anonimna Firebase prijava preko REST API-ja (isto što radi FirebaseAuth.signInAnonymously na Androidu).
 * uid i refresh token se čuvaju na telefonu → isti učenik ostaje isti uid dok se aplikacija ne obriše.
 */
internal class FirebaseAuthRest(private val http: HttpClient) {

    private val brava = Mutex()
    private var idToken: String? = null
    private var isticeMs = 0L

    /** Kao auth.currentUser?.uid — bez mreže. */
    val uid: String? get() = LokalniKljucevi.citaj(K_UID)

    /** Prijavljuje anonimno ako već nije; vraća uid. Baca izuzetak ako nema mreže. */
    suspend fun prijavi(): String {
        token()
        return requireNotNull(uid)
    }

    /** Važeći ID token za Firestore zahtjeve (obnavlja se automatski). */
    suspend fun token(): String = brava.withLock {
        idToken?.let { if (sadaMs() < isticeMs - 60_000) return@withLock it }
        val refresh = LokalniKljucevi.citaj(K_REFRESH)
        if (refresh == null) registruj() else obnovi(refresh)
    }

    private suspend fun registruj(): String {
        val odg = http.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FirebaseKonfig.API_KEY}") {
            header(BUNDLE_HEADER, FirebaseKonfig.BUNDLE_ID)
            contentType(ContentType.Application.Json)
            setBody("""{"returnSecureToken":true}""")
        }
        val j = json(odg)
        LokalniKljucevi.pisi(K_UID, j.str("localId"))
        LokalniKljucevi.pisi(K_REFRESH, j.str("refreshToken"))
        return zapamti(j.str("idToken"), j.str("expiresIn"))
    }

    private suspend fun obnovi(refresh: String): String {
        val odg = http.submitForm(
            url = "https://securetoken.googleapis.com/v1/token?key=${FirebaseKonfig.API_KEY}",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refresh)
            }
        ) { header(BUNDLE_HEADER, FirebaseKonfig.BUNDLE_ID) }
        if (odg.status.value == 400) {
            // Korisnik obrisan / token poništen → nova anonimna prijava
            LokalniKljucevi.pisi(K_REFRESH, null)
            LokalniKljucevi.pisi(K_UID, null)
            return registruj()
        }
        val j = json(odg)
        LokalniKljucevi.pisi(K_REFRESH, j.str("refresh_token"))
        LokalniKljucevi.pisi(K_UID, j.str("user_id"))
        return zapamti(j.str("id_token"), j.str("expires_in"))
    }

    private fun zapamti(token: String, expiresIn: String): String {
        idToken = token
        isticeMs = sadaMs() + (expiresIn.toLongOrNull() ?: 3600L) * 1000L
        return token
    }

    private suspend fun json(odg: HttpResponse): JsonObject {
        val tekst = odg.bodyAsText()
        if (!odg.status.isSuccess()) throw IllegalStateException("Auth ${odg.status.value}: $tekst")
        return Json.parseToJsonElement(tekst).jsonObject
    }

    private fun JsonObject.str(k: String): String = requireNotNull(this[k]?.jsonPrimitive?.content) { "Nedostaje $k" }

    private companion object {
        const val K_UID = "firebase_uid"
        const val K_REFRESH = "firebase_refresh_token"
        const val BUNDLE_HEADER = "X-Ios-Bundle-Identifier"
    }
}
