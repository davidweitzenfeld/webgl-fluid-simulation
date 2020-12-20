import ext.compileShader
import glsl.*
import kotlinx.browser.window
import model.DoubleFramebuffer
import model.SelectionPointer
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
    val externalForcesProgram: GLProgram,
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
    val externalForcesShader = gl.compileShader(ExternalForcesShader)

    // Programs
    val displayProgram = GLProgram(gl, vertexShader, displayShader)
    val splatProgram = GLProgram(gl, vertexShader, splatShader)
    val advectionProgram = GLProgram(gl, vertexShader, advectionShader)
    val externalForcesProgram = GLProgram(gl, vertexShader, externalForcesShader)

    // Framebuffers
    setUpBlittingFramebuffers(gl)
    val densityFramebuffer = createDoubleFramebuffer(gl, textureId = 2, width, height)
    val velocityFramebuffer = createDoubleFramebuffer(gl, textureId = 0, width, height)

    // State
    val pointer = SelectionPointer()
    val state = SimState(pointer, densityFramebuffer, velocityFramebuffer, width, height, displayProgram, splatProgram, advectionProgram, externalForcesProgram)

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

        externalForcesProgram.bind()
        gl.uniform1i(externalForcesProgram["uVelocity"], velocityFramebuffer.read.textureId)
        gl.uniform2f(externalForcesProgram["f"], 0f, -9.81f)
        gl.uniform1f(externalForcesProgram["dt"], dt)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()

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
