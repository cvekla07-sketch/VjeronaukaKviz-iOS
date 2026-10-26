package ba.osgrivice.kviz.data.remote.rest

/**
 * Podaci iz GoogleService-Info.plist (iOS aplikacija u Firebase projektu vjeronauka-kviz).
 * Firebase API ključ nije tajna — nalazi se u svakoj objavljenoj aplikaciji; pristup štite Firestore pravila.
 */
internal object FirebaseKonfig {
    const val API_KEY = "AIzaSyDeMKkLMhRxRmoJOySlDIZMxH-n7JOHEdk"
    const val PROJECT_ID = "vjeronauka-kviz"
    const val BUNDLE_ID = "ba.osgrivice.kviz"
}
