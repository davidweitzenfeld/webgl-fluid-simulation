package glsl

import org.khronos.webgl.WebGLRenderingContext as GL

/**
 * Shader for splattering
 */
object SplatterShader : Shader {
    override val type = GL.FRAGMENT_SHADER

    // language=GLSL
    override val content: String = """
        precision highp float;
        precision mediump sampler2D;

        varying vec2 vUv; // Texel position.
        
        uniform sampler2D uTarget; // Target texture.
        uniform float aspectRatio; // Texture aspect ratio.
        uniform vec3 color;
        uniform vec2 point; // Point to splatter around.
        uniform float radius; // Splatter radius.

        void main () {
        
            // Distance between this texel and the splatter point.
            vec2 diffVec = vUv - point.xy;
            diffVec.x *= aspectRatio;
            float dist = dot(diffVec, diffVec);

            // Brighter color closer to point.
            vec3 base = texture2D(uTarget, vUv).xyz;
            vec3 splatter = exp(-dist / radius) * color;
            gl_FragColor = vec4(base + splatter, 1.0);
        }
    """.trimIndent()
}
