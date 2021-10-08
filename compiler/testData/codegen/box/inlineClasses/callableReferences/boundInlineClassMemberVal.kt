// WITH_RUNTIME

@JvmInline
value class Z(val x: Int) {
    val xx get() = x
}

@JvmInline
value class L(val x: Long) {
    val xx get() = x
}

@JvmInline
value class S(val x: String) {
    val xx get() = x
}

fun box(): String {
    if (Z(42)::xx.get() != 42) throw AssertionError()
    if (L(1234L)::xx.get() != 1234L) throw AssertionError()
    if (S("abcdef")::xx.get() != "abcdef") throw AssertionError()

    return "OK"
}