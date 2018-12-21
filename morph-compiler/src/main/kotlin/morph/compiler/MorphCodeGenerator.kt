package morph.compiler

import com.squareup.kotlinpoet.NameAllocator
import morph.api.Morph
import javax.lang.model.element.TypeElement

data class MorphTargetsAndSources(val target: TypeElement, val sources: List<TypeElement> )

class MorphCodeGenerator {

    companion object {
//        data class MorphTargetsAndSources(target: from: Array<KClass<*>>)
    }

    fun generateMorphExtensions(targets: Sequence<TypeElement>) {
        val pairs = targets.map {
            Pair(it, it.getAnnotation(Morph::class.java).from.map { it as TypeElement })
        }
    }

}

fun ValidatedContext.generateMorphExtensions(): Unit {
    sources.forEach {  }

}

private fun generateForTargergetAndSource(target: KotlinClassElement, source: KotlinClassElement) {

}