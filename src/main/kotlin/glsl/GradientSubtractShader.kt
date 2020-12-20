package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * Gradient subtraction from the pressure in order to correct the divergence.
 */
object GradientSubtractShader : Shader {
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
        
        uniform sampler2D pressureTexture;
        uniform sampler2D velocityTexture;

        void main () {
            float left = texture2D(pressureTexture, leftTexel).x;
            float right = texture2D(pressureTexture, rightTexel).x;
            float up = texture2D(pressureTexture, upTexel).x;
            float down = texture2D(pressureTexture, downTexel).x;
            
            vec2 velocity = texture2D(velocityTexture, texel).xy;
            
            vec2 newVelocity = velocity - vec2(right - left, up - down);
            gl_FragColor = vec4(newVelocity, 0.0, 1.0);
        }
    """.trimIndent()
}
