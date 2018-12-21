package com.ltrojanowski.morph

import morph.api.Morph
import morph.api.into

data class Foo(val stringValue: String, var numValue: Int)

@Morph(from = [Foo::class])
data class Boo(val stringValue: String, val numValue: Int)

fun main(args: Array<String>) {

    val foo = Foo("abc", 123)

    val boo = foo.into<Boo>().morph()

}