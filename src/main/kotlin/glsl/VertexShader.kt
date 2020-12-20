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
        varying vec2 texel;
        
        // Edges of texture for use in fragment shader.
        varying vec2 leftTexel;
        varying vec2 rightTexel;
        varying vec2 upTexel;
        varying vec2 downTexel;
        
        // Texture size.
        uniform vec2 texelSize;
        
        vec2 texelAtOffset(in vec2 v, in vec2 offset) {
            return v + offset * texelSize;
        }

        void main () {
            texel = aPosition * 0.5 + 0.5;
            
            leftTexel = texelAtOffset(texel, vec2(-1.0, 0.0));
            rightTexel = texelAtOffset(texel, vec2(1.0, 0.0));
            upTexel = texelAtOffset(texel, vec2(0.0, 1.0));
            downTexel = texelAtOffset(texel, vec2(0.0, -1.0));
            
            gl_Position = vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()
}
