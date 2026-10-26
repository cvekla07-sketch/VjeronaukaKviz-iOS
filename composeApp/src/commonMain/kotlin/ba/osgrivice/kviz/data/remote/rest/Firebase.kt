package ba.osgrivice.kviz.data.remote.rest

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

/** Jedna zajednička veza prema Firebaseu (kao FirebaseApp na Androidu). */
internal object Firebase {
    private val http: HttpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 15_000
            }
        }
    }
    val auth: FirebaseAuthRest by lazy { FirebaseAuthRest(http) }
    val firestore: FirestoreRest by lazy { FirestoreRest(http, auth) }
}
