package model

import Color

data class SelectionPointer(
    val down: Boolean = false,
    val moved: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val color: Color = floatArrayOf(0f, 0f, 0f)
)
