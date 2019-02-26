package morph.compiler

sealed class Validated<out E, out R> {
    fun isValid(): Boolean { return this is Valid }
    fun isInvalid(): Boolean { return this is Invalid }
}

data class Valid<out R>(val value: R) : Validated<Nothing, R>()
data class Invalid<out E>(val value: List<E>) : Validated<E, Nothing>()

fun <E, R1, R2, R3> Validated<E, R1>.combine(with: Validated<E, R2>, fn: (R1, R2) -> R3): Validated<E, R3> {
    return when {
        this is Invalid<E> && with is Invalid<E> ->
            Invalid(this.value + with.value)
        this is Invalid<E> && with is Valid<R2> ->
            Invalid(this.value)
        this is Valid<R1> && with is Invalid<E> ->
            Invalid(with.value)
        this is Valid<R1> && with is Valid<R2> ->
            Valid(fn(this.value, with.value))
        else -> throw IllegalStateException("Illegal state reached when combining Validated")
    }
}

fun <E, R1, R2> Validated<E, R1>.combineRight(block: (R1) -> Validated<E, R2>): Validated<E, R2> {
    return when {
        this is Invalid<E> -> this
        this is Valid<R1> -> block(this.value)
        else -> throw IllegalStateException("Illegal state reached when combining Validated")
    }
}

val unit: (Any?, Any?) -> Unit = { r1, r2 -> Unit }
fun <R1, R2> keepLeft(r1: R1, r2: R2): R1 { return r1 }
fun <R1, R2> keepRight(r1: R1, r2: R2): R2 = r2
