package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph


@Morph(from = [SourceForTargetWithNoOverlappingNames::class])
data class TargetHasNoOverlappingNames(val a: String, val b: Int, val c: Double)

@Morph(from = [SourceForTargetWithNoOverlappingTypes::class])
data class TargetHasNoOverlappingTypes(val a: String, val b: Int, val c: Double)

data class SourceForTargetWithNoOverlappingNames(val d: String, val e: Int, val f: Double)
data class SourceForTargetWithNoOverlappingTypes(val a: Float, val b: Float, val c: Float)