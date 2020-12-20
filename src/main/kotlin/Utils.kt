import kotlin.random.Random

typealias Color = FloatArray

fun color(r: Float, g: Float, b: Float) = floatArrayOf(r, g, b)

fun randomColor(): Color = color(r = Random.nextFloat() * 10, g = Random.nextFloat() * 10, b = Random.nextFloat() * 10)

val Color.r get() = get(0)
val Color.g get() = get(1)
val Color.b get() = get(2)
