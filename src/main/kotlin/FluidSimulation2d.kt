import ext.compileShader
import ext.float32ArrayOf
import ext.textureType
import ext.uint16ArrayOf
import glsl.AdvectionShader
import glsl.DisplayShader
import glsl.SplatterShader
import glsl.VertexShader
import kotlinx.browser.window
import model.DoubleFramebuffer
import model.Framebuffer
import model.SelectionPointer
import org.khronos.webgl.WebGLFramebuffer
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import org.khronos.webgl.WebGLRenderingContext as GL

data class SimState(
    var pointer: SelectionPointer,
    var densityFramebuffer: DoubleFramebuffer,
    var velocityFramebuffer: DoubleFramebuffer,
    var width: Int,
    var height: Int,
    val displayProgram: GLProgram,
    val splatProgram: GLProgram,
    val advectionProgram: GLProgram,
)

fun init2dFluidSimulation(canvas: HTMLCanvasElement, gl: GL): SimState {
    val (width, height) = gl.drawingBufferWidth to gl.drawingBufferHeight

    gl.clearColor(red = 0f, green = 0f, blue = 0f, alpha = 1f)
    gl.viewport(x = 0, y = 0, width, height)

    // Shaders
    val vertexShader = gl.compileShader(VertexShader)
    val splatShader = gl.compileShader(SplatterShader)
    val displayShader = gl.compileShader(DisplayShader)
    val advectionShader = gl.compileShader(AdvectionShader)

    // Programs
    val displayProgram = GLProgram(gl, vertexShader, displayShader)
    val splatProgram = GLProgram(gl, vertexShader, splatShader)
    val advectionProgram = GLProgram(gl, vertexShader, advectionShader)

    // Framebuffers
    setUpBlittingFramebuffers(gl)
    val densityFramebuffer = createDoubleFramebuffer(gl, textureId = 2, width, height)
    val velocityFramebuffer = createDoubleFramebuffer(gl, textureId = 0, width, height)

    // State
    val pointer = SelectionPointer()
    val state = SimState(pointer, densityFramebuffer, velocityFramebuffer, width, height, displayProgram, splatProgram, advectionProgram)

    // Selection pointer events.
    canvas.addEventListener("mousemove", { event ->
        event.preventDefault()
        event as MouseEvent
        state.pointer = state.pointer.copy(
            x = event.offsetX.toFloat(), y = event.offsetY.toFloat(),
            dx = (event.offsetX - state.pointer.x).toFloat() * 100,
            dy = (event.offsetY - state.pointer.y).toFloat() * 100,
        )
    })
    canvas.addEventListener("mousedown", {
        state.pointer = state.pointer.copy(down = true, color = randomColor())
    })
    canvas.addEventListener("mouseup", { state.pointer = state.pointer.copy(down = false) })

    return state
}

fun run2dFluidSimulation(gl: GL, state: SimState) {
    val dt = 0.016f

    with(state) {
        gl.viewport(x = 0, y = 0, width, height)

        advectionProgram.bind()
        gl.uniform2f(advectionProgram["texelSize"], 1f / width, 1f / height)
        gl.uniform1i(advectionProgram["uVelocity"], velocityFramebuffer.read.textureId)
        gl.uniform1i(advectionProgram["uSource"], velocityFramebuffer.read.textureId)
        gl.uniform1f(advectionProgram["dt"], dt)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()
        gl.uniform1i(advectionProgram["uVelocity"], velocityFramebuffer.read.textureId)
        gl.uniform1i(advectionProgram["uSource"], densityFramebuffer.read.textureId)
        blit(gl, densityFramebuffer.write.framebuffer)
        densityFramebuffer.swap()

        if (pointer.down) {
            splat(gl, state, pointer.x, pointer.y, pointer.dx, pointer.dy, pointer.color)
        }

        gl.viewport(x = 0, y = 0, width, height)

        displayProgram.bind()
        gl.uniform1i(displayProgram["uTexture"], densityFramebuffer.read.textureId)
        blit(gl, null)
    }

    window.requestAnimationFrame { run2dFluidSimulation(gl, state) }
}

fun splat(gl: GL, state: SimState, x: Float, y: Float, dx: Float, dy: Float, color: Color) = with(state) {
    splatProgram.bind()
    gl.uniform1i(splatProgram.uniforms["uTarget"]!!, velocityFramebuffer.read.textureId)
    gl.uniform1f(splatProgram.uniforms["aspectRatio"]!!, width / height.toFloat())
    gl.uniform2f(splatProgram.uniforms["point"]!!, x / width, 1f - y / height)
    gl.uniform3f(splatProgram.uniforms["color"]!!, dx, -dy, 1f)
    gl.uniform1f(splatProgram.uniforms["radius"]!!, 0.005f)
    blit(gl, state.velocityFramebuffer.write.framebuffer)
    velocityFramebuffer.swap()

    gl.uniform1i(splatProgram.uniforms["uTarget"]!!, densityFramebuffer.read.textureId)
    gl.uniform3f(splatProgram.uniforms["color"]!!, color.r, color.g, color.b)
    blit(gl, densityFramebuffer.write.framebuffer)
    densityFramebuffer.swap()
}


/**
 * Sets up "blitting" framebuffers used for final drawing.
 */
fun setUpBlittingFramebuffers(gl: GL) {
    // Rectangle of size 2 by 2 centered at (0, 0)
    val rectangle = float32ArrayOf((-1f to -1f), (-1f to 1f), (1f to 1f), (1f to -1f))
    gl.bindBuffer(GL.ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(GL.ARRAY_BUFFER, data = rectangle, GL.STATIC_DRAW)

    // Two triangles making up the rectangle above.
    val triangleIndices = uint16ArrayOf(0, 1, 2, 0, 2, 3)
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, gl.createBuffer())
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER, data = triangleIndices, GL.STATIC_DRAW)
    gl.vertexAttribPointer(/* aPosition */ index = 0, size = 2, type = GL.FLOAT, normalized = false, stride = 0, offset = 0)
    gl.enableVertexAttribArray(index = 0)
}

fun blit(gl: GL, destination: WebGLFramebuffer?) {
    gl.bindFramebuffer(GL.FRAMEBUFFER, destination)
    gl.drawElements(GL.TRIANGLES, 6, GL.UNSIGNED_SHORT, 0)
}

fun createFramebuffer(
    gl: GL, textureId: Int, w: Int, h: Int,
    internalFormat: Int = GL.RGBA, format: Int = GL.RGBA, textureType: Int = gl.textureType, filter: Int = GL.LINEAR,
): Framebuffer {
    gl.activeTexture(GL.TEXTURE0 + textureId)
    val texture = gl.createTexture()!!
    gl.bindTexture(GL.TEXTURE_2D, texture)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, filter)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, filter)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
    gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
    gl.texImage2D(GL.TEXTURE_2D, level = 0, internalFormat, w, h, border = 0, format, textureType, pixels = null)

    val framebuffer = gl.createFramebuffer()!!
    gl.bindFramebuffer(GL.FRAMEBUFFER, framebuffer)
    gl.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.TEXTURE_2D, texture, level = 0)
    gl.viewport(x = 0, y = 0, w, h)
    gl.clear(GL.COLOR_BUFFER_BIT)

    return Framebuffer(framebuffer, texture, textureId)
}

fun createDoubleFramebuffer(
    gl: GL, textureId: Int, w: Int, h: Int,
    internalFormat: Int = GL.RGBA, format: Int = GL.RGBA, textureType: Int = gl.textureType, filter: Int = GL.LINEAR,
) = DoubleFramebuffer(
    write = createFramebuffer(gl, textureId, w, h, internalFormat, format, textureType, filter),
    read = createFramebuffer(gl, textureId + 1, w, h, internalFormat, format, textureType, filter),
)
