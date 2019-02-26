package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph

@Morph(from = [NotKotlinClass::class])
data class TargetWithJavaSources(val foo: String)