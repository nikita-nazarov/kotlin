class A {
    inline fun inlineFun(s: (s: Int) -> Unit, p : Int) {
        s(11)

        s(p)
    }

    fun foo() {
        inlineFun ({ l ->
                       var zzz = l;
                       zzz++
                   }, 11)
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=*
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\3\0 TYPE=I INDEX=*
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=*
// VARIABLE : NAME=l\2 TYPE=I INDEX=*
// VARIABLE : NAME=l\3 TYPE=I INDEX=*
// VARIABLE : NAME=p\1 TYPE=I INDEX=*
// VARIABLE : NAME=this TYPE=LA; INDEX=*
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=*
// VARIABLE : NAME=zzz\2 TYPE=I INDEX=*
// VARIABLE : NAME=zzz\3 TYPE=I INDEX=*
