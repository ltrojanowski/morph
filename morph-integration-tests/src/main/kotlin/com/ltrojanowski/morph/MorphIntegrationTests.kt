package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph

// target
@Morph(from = [Boo::class, Baz::class])
data class Foo(val a: String, val b: Double, val c: Int, val d: Float, val e: List<String>)

// sources
data class Boo(val a: String, val b: Double, val c: Int, val d: Float, val e: List<String>)
data class Baz(val a: String, val b: Double, val c: Int, val d: Float)

fun main(args: Array<String>) {

    val boo = Boo("a", 1.0, 2, 3.0f, listOf("from boo"))
    val baz = Baz("a", 1.0, 2, 3.0f)

    val foo1 = boo.into<Foo>{}.morph()
    assert(foo1.a == boo.a)
    assert(foo1.b == boo.b)
    assert(foo1.c == boo.c)
    assert(foo1.d == boo.d)
    assert(foo1.e == boo.e)
    val foo2 = baz.into<Foo>{
        e = listOf("inserted manually")
    }.morph()
    assert(foo2.a == boo.a)
    assert(foo2.b == boo.b)
    assert(foo2.c == boo.c)
    assert(foo2.d == boo.d)
    assert(foo2.e == listOf("inserted manually"))

}