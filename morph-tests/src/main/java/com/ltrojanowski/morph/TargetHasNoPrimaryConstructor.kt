package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph

@Morph(from = [ValidSourceDataClass::class])
class TargetHasNoPrimaryConstructor {

    private var a: String
    private var b: Int
    private var c: Double

    constructor(a: String, b: Int, c: Double) {
        this.a = a
        this.b = b
        this.c = c
    }

    fun aFunction() {
        println("a function was called")
    }
}