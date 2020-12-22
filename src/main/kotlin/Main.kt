import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.create
import kotlinx.html.id
import kotlinx.html.js.canvas
import org.khronos.webgl.WebGLRenderingContext as GL


fun main() {
    window.addEventListener("load", {
        val canvas = document.create.canvas {
            id = "canvas"
            width = "800"
            height = "600"
        }
        document.body!!.append(canvas)

        val gl = canvas.getContext("webgl") as GL
        val state = init2dFluidSimulation(canvas, gl)
        run2dFluidSimulation(gl, state)
    })
}
