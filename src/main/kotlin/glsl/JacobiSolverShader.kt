package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * A simple iterative solver for Poisson equations.
 */
object JacobiSolverShader : Shader {
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
        
        // Solver parameters.
        uniform float alpha;
        uniform float beta;
        
        // Solver inputs.
        uniform sampler2D xTexture;
        uniform sampler2D bTexture;
        
        vec2 bounded(in vec2 v) {
            return min(max(v, 0.0), 1.0);
        }

        void main () {
            float current = texture2D(xTexture, texel).x;

            float left = texture2D(xTexture, bounded(leftTexel)).x;
            float right = texture2D(xTexture, bounded(rightTexel)).x;
            float up = texture2D(xTexture, bounded(upTexel)).x;
            float down = texture2D(xTexture, bounded(downTexel)).x;

            float b = texture2D(bTexture, texel).x;

            float next = (left + right + up + down + alpha * b) / beta;
            gl_FragColor = vec4(next, 0.0, 0.0, 1.0);
        }
    """.trimIndent()
}
