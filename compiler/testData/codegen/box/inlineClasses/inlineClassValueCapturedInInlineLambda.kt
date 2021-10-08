// WITH_RUNTIME

@JvmInline
value class Z(val int: Int)
@JvmInline
value class L(val long: Long)
@JvmInline
value class Str(val string: String)
@JvmInline
value class Obj(val obj: Any)

fun box(): String {
    var xz = Z(0)
    var xl = L(0L)
    var xs = Str("")
    var xo = Obj("")

    run {
        xz = Z(42)
        xl = L(1234L)
        xs = Str("abc")
        xo = Obj("def")
    }

    if (xz.int != 42) throw AssertionError()
    if (xl.long != 1234L) throw AssertionError()
    if (xs.string != "abc") throw AssertionError()
    if (xo.obj != "def") throw AssertionError()

    return "OK"
}