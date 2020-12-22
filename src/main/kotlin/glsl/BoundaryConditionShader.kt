package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * Enforces boundary conditions by setting boundary values to scaled values of neighbors.
 */
object BoundaryConditionShader : Shader {
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
        
        uniform float scale;
        uniform sampler2D texture;
        uniform vec2 texelSize; 
        
        void main () {
            if (leftTexel.x < texelSize.x) {
                gl_FragColor = scale * texture2D(texture, rightTexel);
            } else if (rightTexel.x > 1.0 - texelSize.x) {
                gl_FragColor = scale * texture2D(texture, leftTexel);
            } else if (upTexel.y > 1.0 - texelSize.y) {
                gl_FragColor = scale * texture2D(texture, downTexel);
           } else if (downTexel.y < texelSize.y) {
                gl_FragColor = scale * texture2D(texture, upTexel);
           } else {
                gl_FragColor = texture2D(texture, texel); 
            }
        }
    """.trimIndent()
}
