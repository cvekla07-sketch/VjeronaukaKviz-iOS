package ba.osgrivice.kviz.domain.kviz

/** Niz dana: broj uzastopnih dana s aktivnošću koji se završava danas ili juče. */
object Streak {
    fun izracunaj(aktivniDani: List<Long>, danas: Long): Int {
        val set = aktivniDani.toHashSet()
        var dan = if (set.contains(danas)) danas else if (set.contains(danas - 1)) danas - 1 else return 0
        var niz = 0
        while (set.contains(dan)) { niz++; dan-- }
        return niz
    }
}
