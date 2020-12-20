package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object ExternalForcesShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 texel;
        uniform sampler2D velocityTexture;
        uniform vec2 f;
        uniform float dt;

        void main () {
            // Velocity update.
            vec2 v = texture2D(velocityTexture, texel).xy; // Velocity.
            vec2 vNext = v + dt * f;
            
            gl_FragColor = vec4(vNext, 0.0, 1.0);
        }
    """.trimIndent()
}
