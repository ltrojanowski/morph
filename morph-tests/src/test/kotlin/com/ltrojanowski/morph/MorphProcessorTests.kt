package com.ltrojanowski.morph

import morph.api.Morph
import morph.api.into
import org.junit.Test

class MorphProcessorTests {

    @Test
    fun regular() {
        val foo = Foo("abc", 123, 1.5)
        val boo = foo.into<Boo>().morph()
        assert(boo == Boo("abc", 123, 1.5))
    }

    @Morph(from = [Boo::class])
    internal data class Foo(val v1: String, val v2: Int, val v3: Double)

    internal data class Boo(val v1: String, val v2: Int, val v3: Double)
}