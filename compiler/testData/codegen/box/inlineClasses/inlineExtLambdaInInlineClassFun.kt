// WITH_RUNTIME

inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

@JvmInline
value class R(private val r: Int) {
    fun test() = runInlineExt { "OK" }
}

fun box() = R(0).test()