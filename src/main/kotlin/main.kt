import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.create
import kotlinx.html.id
import kotlinx.html.js.canvas
import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL

fun main() {
    val canvas = document.create.canvas {
        id = "gl-canvas"
        width = "640"
        height = "480"
    }

    window.onload = {
        document.getElementById("container")!!.append(canvas)
        prepareCanvas(canvas)
    }
}

fun prepareCanvas(canvas: HTMLCanvasElement) {
    val gl = canvas.getContext("webgl") as GL
    gl.clearColor(0f, 0f, 0f, 1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT)
}
