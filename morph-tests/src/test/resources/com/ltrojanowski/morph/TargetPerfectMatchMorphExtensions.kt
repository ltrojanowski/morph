// Generated by morph. Do not edit.
package com.ltrojanowski.morph

import com.ltrojanowski.morph.api.MorphBuilder
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit

class TargetPerfectMatchMorphBuilder(
    var a: Int?,
    var b: String?,
    var c: Double?,
    var d: Boolean?
) : MorphBuilder<TargetPerfectMatch> {
    override fun morph(): TargetPerfectMatch = TargetPerfectMatch(a!!, b, c!!, d!!)
}

fun <TargetPerfectMatch> SourcePerfectMatchA.into(block: TargetPerfectMatchMorphBuilder.() -> Unit):
        TargetPerfectMatchMorphBuilder = TargetPerfectMatchMorphBuilder(this.a, this.b, this.c,
        this.d)

fun <TargetPerfectMatch> SourcePerfectMatchB.into(block: TargetPerfectMatchMorphBuilder.() -> Unit):
        TargetPerfectMatchMorphBuilder = TargetPerfectMatchMorphBuilder(this.a, this.b, this.c,
        this.d)
