package ba.osgrivice.kviz.domain.kviz

/** Pravila XP-a na jednom mjestu — da se kasnije mijenjaju bez diranja UI-ja. */
object Bodovanje {
    const val XP_TACAN = 10
    const val XP_BONUS_NIZ = 5          // svaki tačan nakon 3 uzastopna tačna
    const val XP_BONUS_SESIJA_BEZ_GRESKE = 20

    fun xpZaOdgovor(tacno: Boolean, uzastopnoTacnih: Int): Int =
        if (!tacno) 0 else XP_TACAN + if (uzastopnoTacnih >= 3) XP_BONUS_NIZ else 0

    fun bonusKrajSesije(tacnih: Int, ukupno: Int): Int =
        if (ukupno > 0 && tacnih == ukupno) XP_BONUS_SESIJA_BEZ_GRESKE else 0
}
