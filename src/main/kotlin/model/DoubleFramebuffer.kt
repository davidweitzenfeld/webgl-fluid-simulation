package model

data class DoubleFramebuffer(
    var write: Framebuffer,
    var read: Framebuffer,
) {
    fun swap() {
        val temp = write
        write = read
        read = temp
    }
}
