import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.create
import kotlinx.html.id
import kotlinx.html.js.canvas
import org.khronos.webgl.WebGLRenderingContext as GL


fun main() {
    window.onload = {
        val canvas = document.create.canvas {
            id = "canvas"
            width = "600"
            height = "400"
        }
        document.body!!.append(canvas)

        val gl = canvas.getContext("webgl") as GL
        shader(gl, canvas)
    }
}
