package ba.osgrivice.kviz.data.remote.rest

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Greška Firestore servera (npr. status = "PERMISSION_DENIED"). */
internal class FirestoreIzuzetak(val status: String, poruka: String) : Exception("$status: $poruka")

/** Oznaka za FieldValue.serverTimestamp(). */
internal object ServerVrijeme

/** Vrijeme iz Firestore Timestamp polja. */
internal class Vrijeme(val ms: Long)

/** Pročitani dokument — isti getteri kao DocumentSnapshot na Androidu. */
internal class Dokument(val id: String, val data: Map<String, Any?>) {
    fun get(polje: String): Any? = data[polje]
    fun getString(polje: String): String? = data[polje] as? String
    fun getLong(polje: String): Long? = (data[polje] as? Number)?.toLong()
    fun getTimestampMs(polje: String): Long? = (data[polje] as? Vrijeme)?.ms
}

/** Jedno pisanje u commit-u (kao batch.set / ref.set na Androidu). */
internal class Pisanje(val putanja: String, val podaci: Map<String, Any?>, val merge: Boolean = false)

internal class Filter(val polje: String, val op: String, val vrijednost: Any?) {
    companion object {
        fun jednako(polje: String, v: Any?) = Filter(polje, "EQUAL", v)
        fun veceOd(polje: String, v: Any?) = Filter(polje, "GREATER_THAN", v)
    }
}

/**
 * Firestore preko REST API-ja (umjesto Android Firestore SDK-a).
 * Pristup štite ista Firestore pravila — zahtjevi idu s ID tokenom anonimnog korisnika.
 */
internal class FirestoreRest(private val http: HttpClient, private val auth: FirebaseAuthRest) {

    private val baza = "projects/${FirebaseKonfig.PROJECT_ID}/databases/(default)/documents"
    private val url = "https://firestore.googleapis.com/v1/$baza"

    /** null ako dokument ne postoji. */
    suspend fun dokument(putanja: String): Dokument? {
        val t = auth.token()
        val odg = http.get("$url/$putanja") { zaglavlja(t) }
        if (odg.status.value == 404) return null
        return uDokument(provjeri(odg).jsonObject)
    }

    /** Svi dokumenti kolekcije (kao collection(...).get()). */
    suspend fun kolekcija(putanja: String): List<Dokument> {
        val rezultat = mutableListOf<Dokument>()
        var stranica: String? = null
        do {
            val t = auth.token()
            val odg = http.get("$url/$putanja") {
                zaglavlja(t)
                parameter("pageSize", 300)
                stranica?.let { parameter("pageToken", it) }
            }
            val j = provjeri(odg).jsonObject
            j["documents"]?.jsonArray?.forEach { rezultat += uDokument(it.jsonObject) }
            stranica = (j["nextPageToken"] as? JsonPrimitive)?.content
        } while (stranica != null)
        return rezultat
    }

    /** Upit nad kolekcijom: where (AND), orderBy, limit. */
    suspend fun upit(
        kolekcijaPutanja: String,
        filteri: List<Filter> = emptyList(),
        poredak: String? = null,
        opadajuce: Boolean = false,
        limit: Int? = null
    ): List<Dokument> {
        val roditelj = kolekcijaPutanja.substringBeforeLast('/', "")
        val kolekcijaId = kolekcijaPutanja.substringAfterLast('/')
        val tijelo = buildJsonObject {
            put("structuredQuery", buildJsonObject {
                put("from", buildJsonArray { add(buildJsonObject { put("collectionId", kolekcijaId) }) })
                if (filteri.isNotEmpty()) {
                    val pojedinacni = filteri.map { f ->
                        buildJsonObject {
                            put("fieldFilter", buildJsonObject {
                                put("field", buildJsonObject { put("fieldPath", f.polje) })
                                put("op", f.op)
                                put("value", uVrijednost(f.vrijednost))
                            })
                        }
                    }
                    put("where", pojedinacni.singleOrNull() ?: buildJsonObject {
                        put("compositeFilter", buildJsonObject {
                            put("op", "AND")
                            put("filters", JsonArray(pojedinacni))
                        })
                    })
                }
                if (poredak != null) {
                    put("orderBy", buildJsonArray {
                        add(buildJsonObject {
                            put("field", buildJsonObject { put("fieldPath", poredak) })
                            put("direction", if (opadajuce) "DESCENDING" else "ASCENDING")
                        })
                    })
                }
                if (limit != null) put("limit", limit)
            })
        }
        val adresa = if (roditelj.isEmpty()) "$url:runQuery" else "$url/$roditelj:runQuery"
        val t = auth.token()
        val odg = http.post(adresa) {
            zaglavlja(t)
            contentType(ContentType.Application.Json)
            setBody(tijelo.toString())
        }
        return provjeri(odg).jsonArray.mapNotNull { red ->
            red.jsonObject["document"]?.jsonObject?.let { uDokument(it) }
        }
    }

    /** Atomski upis više dokumenata (kao WriteBatch.commit()). */
    suspend fun commit(pisanja: List<Pisanje>) {
        if (pisanja.isEmpty()) return
        val tijelo = buildJsonObject {
            put("writes", buildJsonArray {
                pisanja.forEach { p ->
                    val transformacije = mutableListOf<String>()
                    val maska = mutableListOf<String>()
                    skupiPutanje(p.podaci, "", transformacije, maska)
                    add(buildJsonObject {
                        put("update", buildJsonObject {
                            put("name", "$baza/${p.putanja}")
                            put("fields", uPolja(p.podaci))
                        })
                        if (p.merge) {
                            put("updateMask", buildJsonObject {
                                put("fieldPaths", JsonArray(maska.map { JsonPrimitive(it) }))
                            })
                        }
                        if (transformacije.isNotEmpty()) {
                            put("updateTransforms", JsonArray(transformacije.map { putanja ->
                                buildJsonObject {
                                    put("fieldPath", putanja)
                                    put("setToServerValue", "REQUEST_TIME")
                                }
                            }))
                        }
                    })
                }
            })
        }
        val t = auth.token()
        val odg = http.post("$url:commit") {
            zaglavlja(t)
            contentType(ContentType.Application.Json)
            setBody(tijelo.toString())
        }
        provjeri(odg)
    }

    // ---------------------------------------------------------------- pomoćno

    private fun io.ktor.client.request.HttpRequestBuilder.zaglavlja(token: String) {
        bearerAuth(token)
        header("X-Ios-Bundle-Identifier", FirebaseKonfig.BUNDLE_ID)
    }

    private suspend fun provjeri(odg: HttpResponse): JsonElement {
        val tekst = odg.bodyAsText()
        if (!odg.status.isSuccess()) {
            val greska = runCatching { Json.parseToJsonElement(tekst).jsonObject["error"]?.jsonObject }.getOrNull()
            val status = (greska?.get("status") as? JsonPrimitive)?.content ?: "HTTP_${odg.status.value}"
            val poruka = (greska?.get("message") as? JsonPrimitive)?.content ?: tekst.take(200)
            throw FirestoreIzuzetak(status, poruka)
        }
        return if (tekst.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(tekst)
    }

    /** Leaf putanje polja (za merge masku) i polja sa ServerVrijeme (za transformacije). */
    private fun skupiPutanje(mapa: Map<String, Any?>, prefiks: String, transformacije: MutableList<String>, maska: MutableList<String>) {
        for ((k, v) in mapa) {
            val putanja = if (prefiks.isEmpty()) k else "$prefiks.$k"
            when {
                v === ServerVrijeme -> transformacije += putanja
                v is Map<*, *> && v.isNotEmpty() -> {
                    @Suppress("UNCHECKED_CAST")
                    skupiPutanje(v as Map<String, Any?>, putanja, transformacije, maska)
                }
                else -> maska += putanja
            }
        }
    }

    private fun uPolja(mapa: Map<*, *>): JsonObject = buildJsonObject {
        for ((k, v) in mapa) {
            if (v === ServerVrijeme) continue
            put(k.toString(), uVrijednost(v))
        }
    }

    private fun uVrijednost(v: Any?): JsonObject = when (v) {
        null -> buildJsonObject { put("nullValue", JsonNull) }
        is Boolean -> buildJsonObject { put("booleanValue", v) }
        is Int, is Long, is Short, is Byte -> buildJsonObject { put("integerValue", v.toString()) }
        is Float, is Double -> buildJsonObject { put("doubleValue", (v as Number).toDouble()) }
        is String -> buildJsonObject { put("stringValue", v) }
        is Map<*, *> -> buildJsonObject { put("mapValue", buildJsonObject { put("fields", uPolja(v)) }) }
        is List<*> -> buildJsonObject {
            put("arrayValue", buildJsonObject { put("values", JsonArray(v.map { uVrijednost(it) })) })
        }
        else -> buildJsonObject { put("stringValue", v.toString()) }
    }

    private fun uDokument(j: JsonObject): Dokument {
        val ime = (j["name"] as JsonPrimitive).content
        val polja = j["fields"]?.jsonObject ?: JsonObject(emptyMap())
        return Dokument(ime.substringAfterLast('/'), polja.mapValues { iz(it.value.jsonObject) })
    }

    private fun iz(v: JsonObject): Any? {
        val (tip, sadrzaj) = v.entries.firstOrNull() ?: return null
        return when (tip) {
            "nullValue" -> null
            "booleanValue" -> sadrzaj.jsonPrimitive.content.toBoolean()
            "integerValue" -> sadrzaj.jsonPrimitive.content.toLong()
            "doubleValue" -> sadrzaj.jsonPrimitive.content.toDouble()
            "stringValue", "referenceValue" -> sadrzaj.jsonPrimitive.content
            "timestampValue" -> Vrijeme(Instant.parse(sadrzaj.jsonPrimitive.content).toEpochMilliseconds())
            "mapValue" -> (sadrzaj.jsonObject["fields"]?.jsonObject ?: JsonObject(emptyMap())).mapValues { iz(it.value.jsonObject) }
            "arrayValue" -> sadrzaj.jsonObject["values"]?.jsonArray?.map { iz(it.jsonObject) } ?: emptyList<Any?>()
            else -> sadrzaj.toString()
        }
    }
}
