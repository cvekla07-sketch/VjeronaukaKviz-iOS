package ba.osgrivice.kviz.platform

/** Trenutno vrijeme u milisekundama (zamjena za System.currentTimeMillis()). */
expect fun sadaMs(): Long

/** Unicode NFD dekompozicija (zamjena za java.text.Normalizer, Form.NFD). */
expect fun nfd(s: String): String

/** Zamjena za android.util.Log — ispis u Xcode konzolu. */
object Log {
    fun d(tag: String, msg: String) = println("D/$tag: $msg")
    fun i(tag: String, msg: String) = println("I/$tag: $msg")
    fun w(tag: String, msg: String) = println("W/$tag: $msg")
    fun e(tag: String, msg: String, t: Throwable? = null) = println("E/$tag: $msg ${t?.message ?: ""}")
}

/** DataStore fajl "<ime>.preferences_pb" u folderu aplikacije (zamjena za Context.preferencesDataStore). */
expect fun kreirajDataStore(ime: String): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

/** versionName aplikacije (iOS: CFBundleShortVersionString). */
expect fun verzijaAplikacije(): String
