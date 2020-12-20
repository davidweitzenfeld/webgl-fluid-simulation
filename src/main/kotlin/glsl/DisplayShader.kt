package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object DisplayShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 vUv;
        uniform sampler2D uTexture;

        void main () {
            gl_FragColor = texture2D(uTexture, vUv);
        }
    """.trimIndent()
}
