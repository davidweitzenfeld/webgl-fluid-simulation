package model

data class Pointer(
    var down: Boolean,
    var moved: Boolean,
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var color: Array<Float> = arrayOf(0f, 0f, 0f)
) {
}
