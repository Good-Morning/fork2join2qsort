import java.util.concurrent.*

class QSortDouble(private val pool: ForkJoinPool, private val block: Int = 25_000_000) {

    private val random = ThreadLocalRandom.current()

    private fun seq(vector: DoubleArray, l: Int, r: Int) {
        if (l < r - 1) {
            val x = vector[random.nextInt(l, r)]
            var ll = l
            var rr = r - 1
            var lll = ll
            var rrr = rr
            while (lll <= rrr) {
                when {
                    vector[lll] == x -> {
                        lll++
                    }
                    vector[rrr] == x -> {
                        rrr--
                    }
                    vector[lll] < x -> {
                        vector[ll] = vector[lll].also {vector[lll] = vector[ll]}
                        ll++
                        lll++
                    }
                    vector[rrr] > x -> {
                        vector[rr] = vector[rrr].also {vector[rrr] = vector[rr]}
                        rr--
                        rrr--
                    }
                    else -> {
                        vector[ll] = vector[lll].also{vector[lll] = vector[ll]}
                        lll++
                        vector[rr] = vector[rrr].also{vector[rrr] = vector[rr]}
                        rrr--
                        vector[ll] = vector[rr].also{vector[rr] = vector[ll]}
                        ll++
                        rr--
                    }
                }
            }
            seq(vector, l, ll)
            seq(vector, rr+1, r)
        }
    }

    private fun fork2join(f: () -> Unit, g: () -> Unit) {
        var p: ForkJoinTask<*>? = null
        if (pool.parallelism - pool.activeThreadCount > 0) {
            try {
                p = pool.submit(f)
            } catch (e: RejectedExecutionException) {
                f()
            }
        } else {
            f()
        }
        g()
        p?.join()
    }

    fun seq(vector: DoubleArray) {
        seq(vector, 0, vector.size)
    }

    private fun pfor(l: Int, r: Int, f: (Int) -> Unit) {
        if (l < r - block) {
            val m = (l + r) / 2
            fork2join(
                    {pfor(l, m, f)},
                    {pfor(m, r, f)})
        } else {
            for (i in l until r) {
                f(i)
            }
        }
    }

    private fun par(vector: DoubleArray, temp: DoubleArray, cmps0: IntArray, cmps1: IntArray, cmps2: IntArray, l: Int, r: Int) {
        if (l < r - block) {
            val x = vector[random.nextInt(l, r)]
            pfor(l, r) {
                temp[it] = vector[it]
                cmps0[it] = 0
                cmps1[it] = 0
                cmps2[it] = 0
                if (vector[it] < x) {
                    cmps0[it] = 1
                } else if (vector[it] > x) {
                    cmps1[it] = 1
                } else {
                    cmps2[it] = 1
                }
            }
            val sum = scan(cmps0, cmps1, cmps2, l, r)
            pfor(l, r) {
                val cur = temp[it]
                val cmpv = when {
                    cur < x -> cmps0
                    cur > x -> cmps1
                    else -> cmps2
                }
                vector[cmpv[it]] = cur
            }
            fork2join({
                par(vector, temp, cmps0, cmps1, cmps2, l, l + sum.less)
            }, {
                par(vector, temp, cmps0, cmps1, cmps2, r - sum.greater, r)
            })
        } else {
            seq(vector, l, r)
        }
    }

    private data class Cmp(
        var less: Int,
        var equal: Int,
        var greater: Int
    )

    fun par(vector: DoubleArray) {
        val temp = DoubleArray(vector.size) {0.0}
        val cmps0 = IntArray(vector.size) {0}
        val cmps1 = IntArray(vector.size) {0}
        val cmps2 = IntArray(vector.size) {0}
        par(vector, temp, cmps0, cmps1, cmps2, 0, vector.size)
    }

    private fun scanUp(v0: IntArray, v1: IntArray, v2: IntArray, l: Int, r: Int) {
        if (l < r - 1) {
            val m = (l + r) / 2
            fork2join({
                scanUp(v0, v1, v2, l, m)
            }, {
                scanUp(v0, v1, v2, m, r)
            })
            v0[r - 1] += v0[m - 1]
            v1[r - 1] += v1[m - 1]
            v2[r - 1] += v2[m - 1]
        }
    }

    private fun scanDown(v0: IntArray, v1: IntArray, v2: IntArray, l: Int, r: Int) {
        if (l < r - 1) {
            val m = (l + r) / 2
            v0[m - 1] = v0[r - 1].also {v0[r - 1] += v0[m - 1]}
            v1[m - 1] = v1[r - 1].also {v1[r - 1] += v1[m - 1]}
            v2[m - 1] = v2[r - 1].also {v2[r - 1] += v2[m - 1]}
            fork2join({
                scanDown(v0, v1, v2, l, m)
            }, {
                scanDown(v0, v1, v2, m, r)
            })
        }
    }

    private fun scan(v0: IntArray, v1: IntArray, v2: IntArray, l: Int, r: Int): Cmp {
        scanUp(v0, v1, v2, l, r)
        val res = Cmp(v0[r - 1], v1[r - 1], v2[r - 1])
        v0[r - 1] = 0
        v1[r - 1] = 0
        v2[r - 1] = 0
        scanDown(v0, v1, v2, l, r)
        return res
    }
}
