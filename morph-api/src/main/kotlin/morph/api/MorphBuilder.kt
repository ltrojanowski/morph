package morph.api

interface MorphBuilder<R> {
    fun morph(): R
}