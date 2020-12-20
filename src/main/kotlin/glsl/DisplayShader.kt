package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object DisplayShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 texel;
        uniform sampler2D texture;

        void main () {
            gl_FragColor = texture2D(texture, texel);
        }
    """.trimIndent()
}
