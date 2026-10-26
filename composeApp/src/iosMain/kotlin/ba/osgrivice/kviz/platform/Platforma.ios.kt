package ba.osgrivice.kviz.platform

import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.timeIntervalSince1970

actual fun sadaMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun nfd(s: String): String = NSString.create(string = s).decomposedStringWithCanonicalMapping
