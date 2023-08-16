class A {
    inline fun inlineFun(s: (s: Int) -> Unit) {
        s(11)
    }

    fun foo() {
        inlineFun ({
                       var zzz = it;
                       zzz++
                   })
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=*
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=*
// VARIABLE : NAME=it\2 TYPE=I INDEX=*
// VARIABLE : NAME=this TYPE=LA; INDEX=*
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=*
// VARIABLE : NAME=zzz\2 TYPE=I INDEX=*
