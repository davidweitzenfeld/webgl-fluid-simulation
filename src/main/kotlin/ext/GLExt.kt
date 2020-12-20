package ext

import glsl.Shader
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLRenderingContext as GL

fun GL.compileShader(shader: Shader): WebGLShader = compileShader(shader.type, shader.content)

fun GL.compileShader(type: Int, source: String): WebGLShader {
    val shader = createShader(type)!!
    shaderSource(shader, source)
    compileShader(shader)

    if (!getParameter<Boolean>(shader, GL.COMPILE_STATUS)) {
        throw Exception(getShaderInfoLog(shader))
    }

    return shader
}

inline fun <reified T> GL.getParameter(program: WebGLProgram, param: Int): T {
    return getProgramParameter(program, param) as T
}

inline fun <reified T> GL.getParameter(shader: WebGLShader, param: Int): T {
    return getShaderParameter(shader, param) as T
}

val GL.textureType get() = getExtension("OES_texture_half_float").HALF_FLOAT_OES as Int

fun float32ArrayOf(vararg elements: Float) = Float32Array(elements.toTypedArray())

fun float32ArrayOf(vararg elements: Pair<Float, Float>) = Float32Array(elements.flatMap { it.toList() }.toTypedArray())

fun uint16ArrayOf(vararg elements: Short) = Uint16Array(elements.toTypedArray())
