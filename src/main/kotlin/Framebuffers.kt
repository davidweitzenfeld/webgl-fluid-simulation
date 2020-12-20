import ext.textureType
import model.DoubleFramebuffer
import model.Framebuffer
import org.khronos.webgl.WebGLRenderingContext

fun createFramebuffer(
    gl: WebGLRenderingContext, textureId: Int, w: Int, h: Int,
    internalFormat: Int = WebGLRenderingContext.RGBA, format: Int = WebGLRenderingContext.RGBA, textureType: Int = gl.textureType, filter: Int = WebGLRenderingContext.LINEAR,
): Framebuffer {
    gl.activeTexture(WebGLRenderingContext.TEXTURE0 + textureId)
    val texture = gl.createTexture()!!
    gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, filter)
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, filter)
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE)
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE)
    gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, level = 0, internalFormat, w, h, border = 0, format, textureType, pixels = null)

    val framebuffer = gl.createFramebuffer()!!
    gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer)
    gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER, WebGLRenderingContext.COLOR_ATTACHMENT0, WebGLRenderingContext.TEXTURE_2D, texture, level = 0)
    gl.viewport(x = 0, y = 0, w, h)
    gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)

    return Framebuffer(framebuffer, texture, textureId)
}

fun createDoubleFramebuffer(
    gl: WebGLRenderingContext, textureId: Int, w: Int, h: Int,
    internalFormat: Int = WebGLRenderingContext.RGBA, format: Int = WebGLRenderingContext.RGBA, textureType: Int = gl.textureType, filter: Int = WebGLRenderingContext.LINEAR,
) = DoubleFramebuffer(
    write = createFramebuffer(gl, textureId, w, h, internalFormat, format, textureType, filter),
    read = createFramebuffer(gl, textureId + 1, w, h, internalFormat, format, textureType, filter),
)
