import ext.compileShader
import glsl.*
import kotlinx.browser.window
import model.DoubleFramebuffer
import model.Framebuffer
import model.SelectionPointer
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import org.khronos.webgl.WebGLRenderingContext as GL

data class SimState(
    var pointer: SelectionPointer,
    var densityFramebuffer: DoubleFramebuffer,
    var velocityFramebuffer: DoubleFramebuffer,
    var divergenceFramebuffer: Framebuffer,
    var pressureFramebuffer: DoubleFramebuffer,
    var width: Int,
    var height: Int,
    val displayProgram: GLProgram,
    val splatProgram: GLProgram,
    val advectionProgram: GLProgram,
    val externalForcesProgram: GLProgram,
    val divergenceProgram: GLProgram,
    val jacobiSolverProgram: GLProgram,
    val gradientSubtractProgram: GLProgram,
    val boundaryConditionProgram: GLProgram,
)

fun init2dFluidSimulation(canvas: HTMLCanvasElement, gl: GL): SimState {
    val (width, height) = gl.drawingBufferWidth to gl.drawingBufferHeight

    gl.clearColor(red = 0.102f, green = 0.102f, blue = 0.102f, alpha = 1f)
    gl.viewport(x = 0, y = 0, width, height)

    // Shaders
    val vertexShader = gl.compileShader(VertexShader)
    val splatShader = gl.compileShader(SplatterShader)
    val displayShader = gl.compileShader(DisplayShader)
    val advectionShader = gl.compileShader(AdvectionShader)
    val externalForcesShader = gl.compileShader(ExternalForcesShader)
    val divergenceShader = gl.compileShader(DivergenceShader)
    val jacobiSolverShader = gl.compileShader(JacobiSolverShader)
    val gradientSubtractShader = gl.compileShader(GradientSubtractShader)
    val boundaryConditionShader = gl.compileShader(BoundaryConditionShader)

    // Programs
    val displayProgram = GLProgram(gl, vertexShader, displayShader)
    val splatProgram = GLProgram(gl, vertexShader, splatShader)
    val advectionProgram = GLProgram(gl, vertexShader, advectionShader)
    val divergenceProgram = GLProgram(gl, vertexShader, divergenceShader)
    val externalForcesProgram = GLProgram(gl, vertexShader, externalForcesShader)
    val jacobiSolverProgram = GLProgram(gl, vertexShader, jacobiSolverShader)
    val gradientSubtractProgram = GLProgram(gl, vertexShader, gradientSubtractShader)
    val boundaryConditionProgram = GLProgram(gl, vertexShader, boundaryConditionShader)

    // Framebuffers
    setUpBlittingFramebuffers(gl)
    val densityFramebuffer = createDoubleFramebuffer(gl, textureId = 2, width, height)
    val velocityFramebuffer = createDoubleFramebuffer(gl, textureId = 0, width, height)
    val divergenceFramebuffer = createFramebuffer(gl, textureId = 4, width, height)
    val pressureFramebuffer = createDoubleFramebuffer(gl, textureId = 5, width, height)

    // State
    val pointer = SelectionPointer()
    val state = SimState(pointer, densityFramebuffer, velocityFramebuffer, divergenceFramebuffer, pressureFramebuffer, width, height,
        displayProgram, splatProgram, advectionProgram, externalForcesProgram, divergenceProgram, jacobiSolverProgram, gradientSubtractProgram, boundaryConditionProgram)

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
        gl.uniform1i(advectionProgram["velocityTexture"], velocityFramebuffer.read.textureId)
        gl.uniform1i(advectionProgram["sourceTexture"], velocityFramebuffer.read.textureId)
        gl.uniform1f(advectionProgram["dt"], dt)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()
        gl.uniform1i(advectionProgram["velocityTexture"], velocityFramebuffer.read.textureId)
        gl.uniform1i(advectionProgram["sourceTexture"], densityFramebuffer.read.textureId)
        blit(gl, densityFramebuffer.write.framebuffer)
        densityFramebuffer.swap()

        if (pointer.down) {
            splat(gl, state, pointer.x, pointer.y, pointer.dx, pointer.dy, pointer.color)
        }

        externalForcesProgram.bind()
        gl.uniform1i(externalForcesProgram["velocityTexture"], velocityFramebuffer.read.textureId)
        gl.uniform2f(externalForcesProgram["f"], 0f, 0f)
        gl.uniform1f(externalForcesProgram["dt"], dt)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()

        divergenceProgram.bind()
        gl.uniform2f(divergenceProgram["texelSize"], 1f / width, 1f / height)
        gl.uniform1i(divergenceProgram["velocityTexture"], velocityFramebuffer.read.textureId)
        gl.uniform1f(divergenceProgram["gridScale"], 1f)
        blit(gl, divergenceFramebuffer.framebuffer)

        jacobiSolverProgram.bind()
        gl.uniform2f(jacobiSolverProgram["texelSize"], 1f / width, 1f / height)
        gl.uniform1f(jacobiSolverProgram["alpha"], -1f)
        gl.uniform1f(jacobiSolverProgram["beta"], 4f)
        gl.uniform1i(jacobiSolverProgram["xTexture"], pressureFramebuffer.read.textureId)
        gl.uniform1i(jacobiSolverProgram["bTexture"], divergenceFramebuffer.textureId)
        gl.activeTexture(GL.TEXTURE0 + pressureFramebuffer.read.textureId)
        repeat(40) {
            gl.bindTexture(GL.TEXTURE_2D, pressureFramebuffer.read.texture)
            blit(gl, pressureFramebuffer.write.framebuffer)
            pressureFramebuffer.swap()
        }

        gradientSubtractProgram.bind()
        gl.uniform2f(gradientSubtractProgram["texelSize"], 1f / width, 1f / height)
        gl.uniform1i(gradientSubtractProgram["pressureTexture"], pressureFramebuffer.read.textureId)
        gl.uniform1i(gradientSubtractProgram["velocityTexture"], velocityFramebuffer.read.textureId)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()

        boundaryConditionProgram.bind()
        gl.uniform2f(boundaryConditionProgram["texelSize"], 1f / width, 1f / height)
        gl.uniform1f(boundaryConditionProgram["scale"], -1f)
        gl.uniform1i(boundaryConditionProgram["texture"], velocityFramebuffer.read.textureId)
        blit(gl, velocityFramebuffer.write.framebuffer)
        velocityFramebuffer.swap()
        gl.bindTexture(GL.TEXTURE_2D, pressureFramebuffer.read.texture)
        gl.uniform1f(boundaryConditionProgram["scale"], 1f)
        gl.uniform1i(boundaryConditionProgram["texture"], pressureFramebuffer.read.textureId)
        blit(gl, pressureFramebuffer.write.framebuffer)
        pressureFramebuffer.swap()

        gl.viewport(x = 0, y = 0, width, height)

        displayProgram.bind()
        gl.uniform1i(displayProgram["texture"], densityFramebuffer.read.textureId)
        blit(gl, null)
    }

    window.requestAnimationFrame { run2dFluidSimulation(gl, state) }
}

fun splat(gl: GL, state: SimState, x: Float, y: Float, dx: Float, dy: Float, color: Color) = with(state) {
    splatProgram.bind()
    gl.uniform1i(splatProgram.uniforms["texture"]!!, velocityFramebuffer.read.textureId)
    gl.uniform1f(splatProgram.uniforms["aspectRatio"]!!, width / height.toFloat())
    gl.uniform2f(splatProgram.uniforms["point"]!!, x / width, 1f - y / height)
    gl.uniform3f(splatProgram.uniforms["color"]!!, dx, -dy, 1f)
    gl.uniform1f(splatProgram.uniforms["radius"]!!, 0.005f)
    blit(gl, state.velocityFramebuffer.write.framebuffer)
    velocityFramebuffer.swap()

    gl.uniform1i(splatProgram.uniforms["texture"]!!, densityFramebuffer.read.textureId)
    gl.uniform3f(splatProgram.uniforms["color"]!!, color.r, color.g, color.b)
    blit(gl, densityFramebuffer.write.framebuffer)
    densityFramebuffer.swap()
}
