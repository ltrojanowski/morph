package morph.api

import java.lang.IllegalStateException

fun <R: Any> Any.into(): MorphBuilder<R> {
    throw IllegalStateException("Didn't generate class")
}

fun <R: Any> Any.into(block: MorphBuilder<R>.() -> Unit): MorphBuilder<R> {
    throw IllegalStateException("Didn't generate class")
}