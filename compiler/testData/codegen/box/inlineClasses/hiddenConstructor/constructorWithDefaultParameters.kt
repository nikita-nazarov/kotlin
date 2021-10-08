// WITH_RUNTIME

@JvmInline
value class S(val string: String)

class Test(val x: S, val y: S = S("K")) {
    val test = x.string + y.string
}

fun box() = Test(S("O")).test