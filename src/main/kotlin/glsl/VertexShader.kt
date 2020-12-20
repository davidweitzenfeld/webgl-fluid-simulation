package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * Base vertex shader for assigning position and sharing values with fragment shader.
 */
object VertexShader : Shader {
    override val type = GL.VERTEX_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        // Texel (texture vector) position.
        attribute vec2 aPosition;
        
        // Texel position for use in fragment shader.
        varying vec2 vUv;
        
        // Edges of texture for use in fragment shader.
        varying vec2 vL;
        varying vec2 vR;
        varying vec2 vT;
        varying vec2 vB;
        
        // Texture size.
        uniform vec2 texelSize;

        void main () {
            vUv = aPosition * 0.5 + 0.5;
            vL = vUv - vec2(texelSize.x, 0.0);
            vR = vUv + vec2(texelSize.x, 0.0);
            vT = vUv + vec2(0.0, texelSize.y);
            vB = vUv - vec2(0.0, texelSize.y);
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()
}
