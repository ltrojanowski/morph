package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph

// target
@Morph(from = [Boo::class, Baz::class, Bar::class])
data class Foo(val b: Double, val a: String, val c: Int, val d: Float, val e: List<String>)

@Morph(from = [Boo::class])
data class Fiz(val b: Double, val a: String, val c: Int, val d: Float, val e: List<String>)


// sources
data class Boo(val a: String, val b: Double, val c: Int, val d: Float, val e: List<String>)
data class Baz(val a: String, val b: Double, val c: Int, val d: Float)
data class Bar(val a: String?, val b: Double?, val c: Int, val d: Float, val e: List<String>)

fun main(args: Array<String>) {

    val boo = Boo("a", 1.0, 2, 3.0f, listOf("from boo"))
    val baz = Baz("a", 1.0, 2, 3.0f)
    val bar = Bar(null, null, 2, 3.0f, listOf("from bar"))

    val fooBuilder: FooMorphBuilder.() -> Unit = {}
    val foo1 = boo.into<Foo>(fooBuilder).morph()
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
    val foo3 = bar.into<Foo>{
        a = a ?: "A if null"
        b = b ?: 0.0
    }.morph()
    assert(foo3.a == "A if null")
    assert(foo3.b == 0.0)
    assert(foo3.c == bar.c)
    assert(foo3.d == bar.d)
    assert(foo3.e == bar.e)
    val fizBuilder: FizMorphBuilder.() -> Unit = {}
    val fiz = boo.into<Fiz>(fizBuilder).morph()
    assert(fiz.a == boo.a)
    assert(fiz.b == boo.b)
    assert(fiz.c == boo.c)
    assert(fiz.d == boo.d)
    assert(fiz.e == boo.e)
}