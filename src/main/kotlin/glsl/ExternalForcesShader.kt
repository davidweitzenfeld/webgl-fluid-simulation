package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object ExternalForcesShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 vUv;
        uniform sampler2D uVelocity;
        uniform vec2 f;
        uniform float dt;

        void main () {
            // Velocity update.
            vec2 v = texture2D(uVelocity, vUv).xy; // Velocity.
            vec2 vNext = v + dt * f;
            
            gl_FragColor = vec4(vNext, 0.0, 1.0);
        }
    """.trimIndent()
}
