package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

object AdvectionShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 vUv;
        uniform sampler2D uVelocity;
        uniform sampler2D uSource;
        uniform vec2 texelSize;
        uniform float dt;
        uniform float dissipation;

        void main () {
            // Position update.
            vec2 v = texture2D(uVelocity, vUv).xy * texelSize; // Velocity.
            vec2 vUvNext = vUv - dt * v;
            
            gl_FragColor = texture2D(uSource, vUvNext);
        }
    """.trimIndent()
}
