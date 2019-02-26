package com.ltrojanowski.morph.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Morph(val from: Array<KClass<*>>)