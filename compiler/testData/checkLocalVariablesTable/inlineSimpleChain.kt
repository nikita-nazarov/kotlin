class A {
    inline fun inlineFun(s: () -> Unit) {
        s()
    }

    fun foo() {
        var s = 0;
        inlineFun {
            var z = 1;
            z++

            inlineFun {
                var zz2 = 2;
                zz2++
            }
        }
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1$1\4\2 TYPE=I INDEX=*
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=*
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=*
// VARIABLE : NAME=$i$f$inlineFun\3 TYPE=I INDEX=*
// VARIABLE : NAME=s TYPE=I INDEX=*
// VARIABLE : NAME=this TYPE=LA; INDEX=*
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=*
// VARIABLE : NAME=this_\3 TYPE=LA; INDEX=*
// VARIABLE : NAME=z\2 TYPE=I INDEX=*
// VARIABLE : NAME=zz2\4 TYPE=I INDEX=*
