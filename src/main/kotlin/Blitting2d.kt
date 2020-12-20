import ext.float32ArrayOf
import ext.uint16ArrayOf
import org.khronos.webgl.WebGLFramebuffer
import org.khronos.webgl.WebGLRenderingContext

/**
 * Sets up "blitting" framebuffers used for final drawing.
 */
fun setUpBlittingFramebuffers(gl: WebGLRenderingContext) {
    // Rectangle of size 2 by 2 centered at (0, 0)
    val rectangle = float32ArrayOf((-1f to -1f), (-1f to 1f), (1f to 1f), (1f to -1f))
    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, data = rectangle, WebGLRenderingContext.STATIC_DRAW)

    // Two triangles making up the rectangle above.
    val triangleIndices = uint16ArrayOf(0, 1, 2, 0, 2, 3)
    gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, data = triangleIndices, WebGLRenderingContext.STATIC_DRAW)
    gl.vertexAttribPointer(/* aPosition */ index = 0, size = 2, type = WebGLRenderingContext.FLOAT, normalized = false, stride = 0, offset = 0)
    gl.enableVertexAttribArray(index = 0)
}

fun blit(gl: WebGLRenderingContext, destination: WebGLFramebuffer?) {
    gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, destination)
    gl.drawElements(WebGLRenderingContext.TRIANGLES, 6, WebGLRenderingContext.UNSIGNED_SHORT, 0)
}
