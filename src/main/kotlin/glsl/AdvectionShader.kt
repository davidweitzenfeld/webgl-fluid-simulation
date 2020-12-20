package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object AdvectionShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 texel;
        uniform sampler2D velocityTexture;
        uniform sampler2D sourceTexture;
        uniform vec2 texelSize;
        uniform float dt;
        uniform float dissipation;

        void main () {
            // Position update.
            vec2 v = texture2D(velocityTexture, texel).xy * texelSize; // Velocity.
            vec2 texelNext = texel - dt * v;
            
            gl_FragColor = texture2D(sourceTexture, texelNext);
        }
    """.trimIndent()
}
