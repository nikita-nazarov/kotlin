// WITH_RUNTIME

@JvmInline
value class R(private val r: Int) {
    fun test() = object {
        override fun toString() = ok()
    }.toString()

    fun ok() = "OK"
}

fun box() = R(0).test()