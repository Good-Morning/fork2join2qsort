import java.util.concurrent.ForkJoinPool
import kotlin.random.Random

fun timer(setup: () -> Unit, func: () -> Unit): Long {
    setup()
    val start = System.currentTimeMillis();
    func()
    val finish = System.currentTimeMillis();
    return finish - start;
}

fun timerN(n: Int, setup: () -> Unit, func: () -> Unit): Long {
    var sum = 0L
    for (i in 1..n) {
        sum += timer(setup, func)
    }
    return sum / n
}

fun main() {
    val n = 100_000_000
    val random = Random(System.currentTimeMillis());
    val qSort = QSortDouble(ForkJoinPool(4))

    val array = DoubleArray(n) {0.0}
    println(timerN(5, {
        for (i in 0 until n) {
            array[i] = random.nextDouble()
        }
    }, {
        qSort.seq(array)
    }))

    println(timerN(5, {
        for (i in 0 until n) {
            array[i] = random.nextDouble()
        }
    }, {
        qSort.par(array)
    }))
}