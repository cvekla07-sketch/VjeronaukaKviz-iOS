package ba.osgrivice.kviz.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.popoverPresentationController

actual fun sadaMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun nfd(s: String): String = NSString.create(string = s).decomposedStringWithCanonicalMapping

/** Documents folder aplikacije (čuva ga iCloud/iTunes backup, kao Android app data). */
@OptIn(ExperimentalForeignApi::class)
fun folderAplikacije(): String {
    val url: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    return requireNotNull(url?.path) { "Documents folder nije dostupan" }
}

actual fun kreirajDataStore(ime: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { "${folderAplikacije()}/$ime.preferences_pb".toPath() }
    )

actual fun verzijaAplikacije(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"

actual object LokalniKljucevi {
    private val ud get() = platform.Foundation.NSUserDefaults.standardUserDefaults
    actual fun citaj(kljuc: String): String? = ud.stringForKey(kljuc)
    actual fun pisi(kljuc: String, vrijednost: String?) {
        if (vrijednost == null) ud.removeObjectForKey(kljuc) else ud.setObject(vrijednost, forKey = kljuc)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun podijeliFajl(sadrzaj: String, imeFajla: String, mimeTip: String, naslov: String) {
    val putanja = platform.Foundation.NSTemporaryDirectory() + imeFajla
    NSString.create(string = sadrzaj).writeToFile(
        putanja, atomically = true, encoding = platform.Foundation.NSUTF8StringEncoding, error = null
    )
    val url = NSURL.fileURLWithPath(putanja)
    val prozor = platform.UIKit.UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    val korijen = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var vrh: platform.UIKit.UIViewController = korijen
    while (vrh.presentedViewController != null) vrh = vrh.presentedViewController!!
    // iPad: prozor mora imati sidro
    prozor.popoverPresentationController?.sourceView = vrh.view
    vrh.presentViewController(prozor, animated = true, completion = null)
}
