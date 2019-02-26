package morph.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal fun TypeName.rawType(): ClassName {
    return when (this) {
        is ClassName -> this
        is ParameterizedTypeName -> rawType
        else -> throw IllegalArgumentException("Cannot get raw type from $this")
    }
}

internal fun TypeName.asNullableIf(condition: Boolean): TypeName {
    return if (condition) this.copy(nullable = true) else this
}
