import ext.getParameter
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLUniformLocation

class GLProgram(private val gl: GL, vertexShader: WebGLShader, fragmentShader: WebGLShader) {

    private val program: WebGLProgram = gl.createProgram()!!.apply {
        gl.attachShader(this, vertexShader)
        gl.attachShader(this, fragmentShader)
        gl.linkProgram(this)
    }

    val uniforms: Map<String, WebGLUniformLocation>

    init {
        if (!gl.getParameter<Boolean>(program, GL.LINK_STATUS)) {
            throw Exception(gl.getProgramInfoLog(program))
        }

        val uniformsCount = gl.getParameter<Int>(program, GL.ACTIVE_UNIFORMS)
        uniforms = (0 until uniformsCount).associate { i ->
            val name = gl.getActiveUniform(program, i)!!.name
            name to gl.getUniformLocation(program, name)!!
        }
    }

    fun bind() {
        gl.useProgram(program)
    }
}
