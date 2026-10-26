package ba.osgrivice.kviz.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ba.osgrivice.kviz.KvizApp
import ba.osgrivice.kviz.domain.model.VrstaSesije
import ba.osgrivice.kviz.ui.screens.admin.AdminScreen
import ba.osgrivice.kviz.ui.screens.admin.AdminUcenikScreen
import ba.osgrivice.kviz.ui.screens.kviz.KvizScreen
import ba.osgrivice.kviz.ui.screens.onboarding.OnboardingScreen
import ba.osgrivice.kviz.ui.screens.pregled.PregledScreen
import ba.osgrivice.kviz.ui.screens.profil.ProfilScreen
import ba.osgrivice.kviz.ui.screens.rang.RangScreen
import ba.osgrivice.kviz.ui.screens.staza.StazaScreen
import ba.osgrivice.kviz.ui.screens.takmicenje.TakmicenjeScreen
import ba.osgrivice.kviz.ui.screens.uputstvo.UputstvoScreen
import ba.osgrivice.kviz.ui.theme.Noc
import kotlinx.serialization.Serializable

@Serializable object OnboardingRuta
@Serializable object StazaRuta
@Serializable object ProfilRuta
@Serializable data class RangRuta(val razred: Int)
@Serializable data class TakmicenjeRuta(val razred: Int)
@Serializable object AdminRuta
@Serializable data class AdminUcenikRuta(val uid: String, val ime: String, val razred: Int)
@Serializable data class KvizRuta(val razred: Int, val vrsta: String, val lekcijaId: String? = null)
@Serializable data class PregledRuta(val razred: Int, val lekcijaId: String)
@Serializable data class UputstvoRuta(val prviPut: Boolean = false)

@Composable
fun KvizNavHost() {
    val app = KvizApp
    val podesen by produceState<Boolean?>(initialValue = null) {
        value = app.container.profilStore.trenutni().podesen
    }
    if (podesen == null) {
        Box(Modifier.fillMaxSize().background(Noc))
        return
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (podesen == true) StazaRuta else OnboardingRuta) {
        composable<OnboardingRuta> {
            OnboardingScreen(naDalje = {
                nav.navigate(UputstvoRuta(prviPut = true)) { popUpTo<OnboardingRuta> { inclusive = true } }
            })
        }
        composable<UputstvoRuta> { backStack ->
            val prviPut = backStack.toRoute<UputstvoRuta>().prviPut
            UputstvoScreen(
                prviPut = prviPut,
                naDalje = {
                    if (prviPut) nav.navigate(StazaRuta) { popUpTo<UputstvoRuta> { inclusive = true } }
                    else nav.popBackStack()
                }
            )
        }
        composable<StazaRuta> {
            StazaScreen(
                naKviz = { razred, vrsta, lekcijaId -> nav.navigate(KvizRuta(razred, vrsta, lekcijaId)) },
                naPregled = { razred, lekcijaId -> nav.navigate(PregledRuta(razred, lekcijaId)) },
                naProfil = { nav.navigate(ProfilRuta) },
                naRang = { razred -> nav.navigate(RangRuta(razred)) },
                naTakmicenje = { razred -> nav.navigate(TakmicenjeRuta(razred)) }
            )
        }
        composable<PregledRuta> { backStack ->
            val r = backStack.toRoute<PregledRuta>()
            PregledScreen(
                razred = r.razred,
                lekcijaId = r.lekcijaId,
                naNazad = { nav.popBackStack() },
                naKviz = {
                    // kviz zamjenjuje pregled na steku — po završetku kviza vraća se na stazu
                    nav.navigate(KvizRuta(r.razred, VrstaSesije.LEKCIJA.kljuc, r.lekcijaId)) {
                        popUpTo<PregledRuta> { inclusive = true }
                    }
                }
            )
        }
        composable<RangRuta> { backStack ->
            RangScreen(razred = backStack.toRoute<RangRuta>().razred, naNazad = { nav.popBackStack() })
        }
        composable<TakmicenjeRuta> { backStack ->
            TakmicenjeScreen(razred = backStack.toRoute<TakmicenjeRuta>().razred, naIzlaz = { nav.popBackStack() })
        }
        composable<ProfilRuta> {
            ProfilScreen(naNazad = { nav.popBackStack() }, naAdmin = { nav.navigate(AdminRuta) }, naUputstvo = { nav.navigate(UputstvoRuta()) })
        }
        composable<AdminRuta> {
            AdminScreen(
                naNazad = { nav.popBackStack() },
                naUcenik = { uid, ime, razred -> nav.navigate(AdminUcenikRuta(uid, ime, razred)) }
            )
        }
        composable<AdminUcenikRuta> { backStack ->
            val r = backStack.toRoute<AdminUcenikRuta>()
            AdminUcenikScreen(uid = r.uid, ime = r.ime, razred = r.razred, naNazad = { nav.popBackStack() })
        }
        composable<KvizRuta> { backStack ->
            val ruta = backStack.toRoute<KvizRuta>()
            KvizScreen(
                razred = ruta.razred,
                vrstaKljuc = ruta.vrsta,
                lekcijaId = ruta.lekcijaId,
                naIzlaz = { nav.popBackStack() },
                naSljedecu = { lekcijaId ->
                    nav.navigate(KvizRuta(ruta.razred, ruta.vrsta, lekcijaId)) { popUpTo<StazaRuta>() }
                }
            )
        }
    }
}
