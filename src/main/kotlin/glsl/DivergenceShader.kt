package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * Computes the divergence of the velocity, as a first step of solving the pressure equation.
 */
object DivergenceShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;
        
        varying vec2 texel;
        varying vec2 leftTexel;
        varying vec2 rightTexel;
        varying vec2 upTexel;
        varying vec2 downTexel;
        
        uniform sampler2D velocityTexture;
        uniform float gridScale;
        
        void main () {
            float left = texture2D(velocityTexture, leftTexel).x;
            float right = texture2D(velocityTexture, rightTexel).x;
            float up = texture2D(velocityTexture, upTexel).y;
            float down = texture2D(velocityTexture, downTexel).y;
            
            float divergence = ((right - left) + (up - down)) / (2.0 * gridScale);
            
            gl_FragColor = vec4(divergence, 0.0, 0.0, 1.0);
        }
    """.trimIndent()
}
