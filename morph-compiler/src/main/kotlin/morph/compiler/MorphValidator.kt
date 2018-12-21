package morph.compiler

import arrow.core.*
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.isPrimary
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import me.eugeniomarletti.kotlin.metadata.visibility
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.*

sealed class ValidationMessage(val kind: Diagnostic.Kind, val msg: String, val element: Element) {
    data class TargetNotKotlinClass(val target: Element) : ValidationMessage(
            ERROR, "@Morph can't be applied to $target: must be a Kotlin class", target)
    data class TargetHasNoSources(val target: Element) : ValidationMessage(
            ERROR, "@Morph applied to $target has empty parameter 'from'", target)
    data class SourcesNotKotlinClasses(val target: Element) : ValidationMessage(
            ERROR, "@Morph can't be applied to $target: not all sources are Kotlin classes", target)
    data class NotDataClass(val target: Element) : ValidationMessage(
            ERROR, "@Morph can't be applied to $target: must be a data class", target)
//    data class NoPrimaryConstructor(val target: Element) : ValidationMessage(
//            ERROR, "@Morph can't be applied to $target: a target or sources lack a primary constructor", target)
    data class MissingPrimaryConstructor(val target: Element) : ValidationMessage(
        ERROR, "@Morph can't be applied to $target: must have a primary constructor", target)
    data class MultipleIdenticalSources(val target: Element) : ValidationMessage(
            ERROR, "@Morph applied to $target has invalid parameter 'from': sources list contains multiple identical elements", target)
    data class NoOverlappingFields(val target: Element, val source: List<Element>) : ValidationMessage(
            ERROR, "@Morph applied to $target has invalid parameter 'from': no fields overlap by name and type for ${source.joinToString()}", target)
}

typealias KotlinProto = ProtoBuf.Class
data class KotlinClassElement(
        val element: TypeElement, val classData: KotlinProto, val nameResolver: NameResolver
)
data class ValidatedContext(val target: KotlinClassElement, val sources: List<KotlinClassElement>)


private fun TypeElement.isKotlinClass(): Option<KotlinClassElement> {
    val typeMetadata = this.kotlinMetadata
    return if (typeMetadata !is KotlinClassMetadata) {
        Option.empty()//.left(ValidationMessage.TargetNotKotlinClass(this))
    } else {
        Option.just(KotlinClassElement(this, typeMetadata.data.classProto, typeMetadata.data.nameResolver))
    }
}

fun KotlinClassElement.hasOverlappingFields(source: KotlinClassElement): Boolean {
    val targetProperties = this.classData.propertyList
            .filter { it.visibility !in MorphProcessor.ALLOWABLE_PROPERTY_VISIBILITY}
            .map {
                Pair(this.classData.getTypeParameter(it.name), this.nameResolver.getString(it.name))
            }.toSet()
    val sourceProperties = source.classData.propertyList.map {
        Pair(source.classData.getTypeParameter(it.name), source.nameResolver.getString(it.name))
    }.toSet()

    return targetProperties.union(sourceProperties).isNotEmpty()
}

fun MorphTargetsAndSources.validate(): Either<List<ValidationMessage>, ValidatedContext> {
    val validationMessage = LinkedList<ValidationMessage>()
    // check if target is kotlin class
    val maybeKotlinClassElementTarget = target.isKotlinClass()
    if (maybeKotlinClassElementTarget.isEmpty()) {
        validationMessage.add(ValidationMessage.TargetNotKotlinClass(target))
    }
    // check if sources is empty list
    if (sources.isEmpty()) {
        validationMessage.add(ValidationMessage.TargetHasNoSources(target))
    }
    // check if sources is kotlin class
    val maybeSources = sources.map { it.isKotlinClass() }
    if (maybeSources.find { it.isEmpty() } == null) {
        validationMessage.add(ValidationMessage.SourcesNotKotlinClasses(target))
    }
    return if (validationMessage.isEmpty()) {
        // valid so far
        val preValidatedContext = ValidatedContext(
                maybeKotlinClassElementTarget.orNull()!!, maybeSources.map { it.orNull()!! }
        )
        // check if target and sources are data classes ??? not sure if necessary ???

        // check if target has primary constructor
        val targetConstructor = preValidatedContext.target.classData.constructorList.find { it.isPrimary }
        if (targetConstructor == null) {
            validationMessage.add(ValidationMessage.MissingPrimaryConstructor(preValidatedContext.target.element))
        }
        // check if source and targets have overlapping fields
        val notOverlapping = preValidatedContext.sources.map { !preValidatedContext.target.hasOverlappingFields(it) }
        if (notOverlapping.isNotEmpty()) {
            validationMessage.add(
                    ValidationMessage.NoOverlappingFields(
                            preValidatedContext.target.element,
                            preValidatedContext.sources.map { it.element }
                    )
            )
        }
        if (validationMessage.isEmpty()) {
            Either.right(preValidatedContext)
        } else {
            Either.left(validationMessage)
        }
    } else {
        Either.left(validationMessage)
    }
}