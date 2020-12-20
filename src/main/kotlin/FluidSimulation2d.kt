import ext.compileShader
import glsl.AdvectionShader
import glsl.DisplayShader
import glsl.SplatterShader
import glsl.VertexShader
import kotlinx.browser.window
import model.DoubleFramebuffer
import model.Framebuffer
import model.Pointer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.WebGLFramebuffer
import org.w3c.dom.HTMLCanvasElement
import kotlin.random.Random
import org.khronos.webgl.WebGLRenderingContext as GL

data class SimState(
    var pointer: Pointer,
    var densityFramebuffer: DoubleFramebuffer,
    var velocityFramebuffer: DoubleFramebuffer,
    var blit: (WebGLFramebuffer?) -> Unit,
    var width: Int,
    var height: Int,
    val displayProgram: GLProgram,
    val splatProgram: GLProgram,
    val advectionProgram: GLProgram,
)

var blitFn: (WebGLFramebuffer?) -> Unit = {}

fun shader(gl: GL, canvas: HTMLCanvasElement) {
    val textureWidth = gl.drawingBufferWidth shr 0
    val textureHeight = gl.drawingBufferHeight shr 0

    gl.clearColor(0f, 0f, 0f, 1f)
    gl.clear(GL.COLOR_BUFFER_BIT)

    // language=GLSL
    val vertexShader = gl.compileShader(VertexShader)
    val splatShader = gl.compileShader(SplatterShader)
    val displayShader = gl.compileShader(DisplayShader)
    val advectionShader = gl.compileShader(AdvectionShader)

    gl.viewport(0, 0, textureWidth, textureHeight)

    val displayProgram = GLProgram(gl, vertexShader, displayShader)
    val splatProgram = GLProgram(gl, vertexShader, splatShader)
    val advectionProgram = GLProgram(gl, vertexShader, advectionShader)

    blitFn = blit(gl)

    val textureType = gl.getExtension("OES_texture_half_float").HALF_FLOAT_OES as Int

    val state = SimState(
        pointer = Pointer(down = false, moved = false, x = 0f, y = 0f, dx = 0f, dy = 0f),
        densityFramebuffer = createDoubleFramebuffer(
            gl, 2, textureWidth, textureHeight, GL.RGBA, GL.RGBA,
            textureType, GL.LINEAR
        ),
        velocityFramebuffer = createDoubleFramebuffer(
            gl, 0, textureWidth, textureHeight, GL.RGBA, GL.RGBA,
            textureType, GL.LINEAR
        ),
        blit = blitFn,
        width = textureWidth,
        height = textureHeight,
        displayProgram = displayProgram,
        splatProgram = splatProgram,
        advectionProgram = advectionProgram,
    )

    canvas.onmousemove = {
        it.preventDefault()
        state.pointer.dx = (it.offsetX - state.pointer.x).toFloat() * 100f
        state.pointer.dy = (it.offsetY - state.pointer.y).toFloat() * 100f
        state.pointer.x = it.offsetX.toFloat()
        state.pointer.y = it.offsetY.toFloat()

        null
    }

    canvas.onmousedown = {
        state.pointer.down = true
        state.pointer.color = arrayOf(
            Random.nextFloat() * 10,
            Random.nextFloat() * 10,
            Random.nextFloat() * 10
        )
        null
    }

    canvas.onmouseup = {
        state.pointer.down = false
        null
    }

    gl.viewport(0, 0, state.width, state.height)
    (0..5).forEach { _ ->
        splat(
            splatProgram, gl, canvas,
            x = Random.nextDouble().toFloat() * canvas.width.toFloat(),
            y = Random.nextDouble().toFloat() * canvas.height.toFloat(),
            dx = 1000 * (Random.nextDouble() - 0.5).toFloat(),
            dy = 1000 * (Random.nextDouble() - 0.5).toFloat(),
            state,
            arrayOf(10f, 20f, 30f)
        )
    }
    update(gl, canvas, state)
}

fun update(gl: GL, canvas: HTMLCanvasElement, state: SimState) {
    val dt = 0.016f

    gl.viewport(0, 0, state.width, state.height)

    state.advectionProgram.bind()
    gl.uniform2f(
        state.advectionProgram.uniforms["texelSize"]!!,
        1f / state.width,
        1f / state.height
    )
    gl.uniform1i(
        state.advectionProgram.uniforms["uVelocity"]!!,
        state.velocityFramebuffer.read.textureId
    )
    gl.uniform1i(
        state.advectionProgram.uniforms["uSource"]!!,
        state.velocityFramebuffer.read.textureId
    )
    gl.uniform1f(state.advectionProgram.uniforms["dt"]!!, dt)
    state.blit(state.velocityFramebuffer.write.framebuffer)
    state.velocityFramebuffer.swap()

    gl.uniform1i(
        state.advectionProgram.uniforms["uVelocity"]!!,
        state.velocityFramebuffer.read.textureId
    )
    gl.uniform1i(
        state.advectionProgram.uniforms["uSource"]!!,
        state.densityFramebuffer.read.textureId
    )
    state.blit(state.densityFramebuffer.write.framebuffer)
    state.densityFramebuffer.swap()

    if (state.pointer.down) {
        println(state.pointer)
        splat(
            state.splatProgram,
            gl,
            canvas,
            state.pointer.x,
            state.pointer.y,
            state.pointer.dx,
            state.pointer.dy,
            state,
            state.pointer.color
        )
    }

    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight)
    state.displayProgram.bind()
    gl.uniform1i(state.displayProgram.uniforms["uTexture"], state.densityFramebuffer.read.textureId)
    blitFn(null)

    window.requestAnimationFrame { update(gl, canvas, state) }
}

fun splat(
    splatProgram: GLProgram,
    gl: GL,
    canvas: HTMLCanvasElement,
    x: Float,
    y: Float,
    dx: Float,
    dy: Float,
    state: SimState,
    color: Array<Float>
) {
    splatProgram.bind()
    gl.uniform1i(splatProgram.uniforms["uTarget"]!!, state.velocityFramebuffer.read.textureId)
    gl.uniform1f(splatProgram.uniforms["aspectRatio"]!!, canvas.width / canvas.height.toFloat())
    gl.uniform2f(splatProgram.uniforms["point"]!!, x / canvas.width, 1f - y / canvas.height)
    gl.uniform3f(splatProgram.uniforms["color"]!!, dx, -dy, 1f)
    gl.uniform1f(splatProgram.uniforms["radius"]!!, 0.005f)
    blitFn(state.velocityFramebuffer.write.framebuffer)
    state.velocityFramebuffer.swap()

    gl.uniform1i(splatProgram.uniforms["uTarget"]!!, state.densityFramebuffer.read.textureId)
    gl.uniform3f(splatProgram.uniforms["color"]!!, color[0], color[1], color[2])
    blitFn(state.densityFramebuffer.write.framebuffer)
    state.densityFramebuffer.swap()
}

fun blit(gl: GL): (WebGLFramebuffer?) -> Unit {
    gl.bindBuffer(GL.ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(
        GL.ARRAY_BUFFER,
        Float32Array(arrayOf(-1f, -1f, -1f, 1f, 1f, 1f, 1f, -1f)),
        GL.STATIC_DRAW
    )

    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, Uint16Array(arrayOf(0, 1, 2, 0, 2, 3)), GL.STATIC_DRAW)
    gl.vertexAttribPointer(/* aPosition */  0, 2, GL.FLOAT, false, 0, 0)
    gl.enableVertexAttribArray(0)

    return { destination: WebGLFramebuffer? ->
        gl.bindFramebuffer(GL.FRAMEBUFFER, destination)
        gl.drawElements(GL.TRIANGLES, 6, GL.UNSIGNED_SHORT, 0)
    }
}


fun createFramebuffer(
    gl: GL, textureId: Int, w: Int, h: Int,
    internalFormat: Int, format: Int, type: Int, filter: Int
): Framebuffer {
    gl.activeTexture(GL.TEXTURE0 + textureId)
    val texture = gl.createTexture()!!
    gl.bindTexture(GL.TEXTURE_2D, texture)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, filter)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, filter)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
    gl.texImage2D(GL.TEXTURE_2D, 0, internalFormat, w, h, 0, format, type, null)

    val framebuffer = gl.createFramebuffer()!!
    gl.bindFramebuffer(GL.FRAMEBUFFER, framebuffer)
    gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.TEXTURE_2D, texture, 0)
    gl.viewport(0, 0, w, h)
    gl.clear(GL.COLOR_BUFFER_BIT)

    return Framebuffer(framebuffer, texture, textureId)
}

fun createDoubleFramebuffer(
    gl: GL, textureId: Int, w: Int, h: Int,
    internalFormat: Int, format: Int, type: Int, filter: Int
) = DoubleFramebuffer(
    write = createFramebuffer(gl, textureId, w, h, internalFormat, format, type, filter),
    read = createFramebuffer(gl, textureId + 1, w, h, internalFormat, format, type, filter),
)
