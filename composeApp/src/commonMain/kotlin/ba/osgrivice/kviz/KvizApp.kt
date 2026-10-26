package ba.osgrivice.kviz

/**
 * iOS zamjena za Android Application klasu.
 * Ekrani koriste KvizApp.container — isto kao (LocalContext.current.applicationContext as KvizApp).container na Androidu.
 */
object KvizApp {
    val container: AppContainer by lazy { AppContainer() }
}
