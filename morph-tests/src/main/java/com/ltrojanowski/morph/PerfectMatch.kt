package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.Morph

@Morph(from = [SourcePerfectMatchA::class, SourcePerfectMatchB::class])
data class TargetPerfectMatch(val a: Int, val b: String?, val c: Double, val d: Boolean)

data class SourcePerfectMatchA(val a: Int, val b: String, val c: Double, val d: Boolean)

data class SourcePerfectMatchB(val a: Int, val b: String, val c: Double, val d: Boolean)