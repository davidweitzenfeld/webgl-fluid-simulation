package model

import org.khronos.webgl.WebGLFramebuffer
import org.khronos.webgl.WebGLTexture

data class Framebuffer(
    val framebuffer: WebGLFramebuffer,
    val texture: WebGLTexture,
    val textureId: Int
)
