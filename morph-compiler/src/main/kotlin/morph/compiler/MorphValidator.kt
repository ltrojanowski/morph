package morph.compiler

import com.squareup.kotlinpoet.TypeName
import me.eugeniomarletti.kotlin.metadata.*
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.*

sealed class ValidationMessage(val kind: Diagnostic.Kind, val msg: String, val element: Element) {
    data class NotKotlinClass(val target: Element) : ValidationMessage(
        ERROR, "@Morph can't be applied to $target: is not a kotlin class", target)
    data class SourceNotKotlinClass(val target: Element, val source: Element): ValidationMessage(
        ERROR, "@Morph applied to $target has invalid members of parameter 'from': $source is not a kotlin class", target)
    data class TargetHasNoSources(val target: Element) : ValidationMessage(
        ERROR, "@Morph applied to $target: has empty parameter 'from'", target)
    data class NoOverlappingFields(val target: Element, val source: Element) : ValidationMessage(
        ERROR, "@Morph applied to $target has invalid parameter 'from': no fields overlap by name and type for $source", target)
    data class MissingPrimaryConstructor(val target: Element) : ValidationMessage(
        ERROR, "@Morph can't be applied to $target: must have a primary constructor", target)
    data class MultipleIdenticalSources(val target: Element) : ValidationMessage(
        ERROR, "@Morph applied to $target has invalid parameter 'from': sources list contains multiple identical elements", target)
}

typealias KotlinProto = ProtoBuf.Class
data class KotlinClassElement(
    val element: TypeElement,
    val classData: KotlinProto,
    val nameResolver: NameResolver
)
data class KotlinTargetsAndSources(val target: KotlinClassElement, val sources: List<KotlinClassElement>)
data class ComputationContext(
        val element: TypeElement,
        val classData: KotlinProto,
        val nameResolver: NameResolver,
        val typeNames: Map<String, TypeName>)
data class ValidatedContext(val target: ComputationContext, val sources: List<ComputationContext>)


private fun TypeElement.validateTargetIsKotlinClass(): Validated<ValidationMessage, KotlinClassElement> {
    val typeMetadata = this.kotlinMetadata
    return if (typeMetadata !is KotlinClassMetadata) {
        Invalid(listOf(ValidationMessage.NotKotlinClass(this)))
    } else {
        Valid(KotlinClassElement(this, typeMetadata.data.classProto, typeMetadata.data.nameResolver))
    }
}

private fun List<TypeElement>.validateSourcesAreNotEmpty(target: TypeElement): Validated<ValidationMessage, Unit> {
    return if (this.isEmpty()) {
        Invalid<ValidationMessage>(listOf(ValidationMessage.TargetHasNoSources(target)))
    } else {
        Valid(Unit)
    }
}

private fun TypeElement.validateSourceIsKotlinClass(target: TypeElement): Validated<ValidationMessage, KotlinClassElement> {
    val typeMetadata = this.kotlinMetadata
    return if (typeMetadata !is KotlinClassMetadata) {
        Invalid(listOf(ValidationMessage.SourceNotKotlinClass(target, this)))
    } else {
        Valid(KotlinClassElement(this, typeMetadata.data.classProto, typeMetadata.data.nameResolver))
    }
}

private fun KotlinClassElement.validatePrimaryConstructor(): Validated<ValidationMessage, ProtoBuf.Constructor> {
    val targetConstructor = this.classData.constructorList.find { it.isPrimary }
    return if (targetConstructor == null) {
        Invalid(listOf(ValidationMessage.MissingPrimaryConstructor(this.element)))
    } else {
        Valid(targetConstructor)
    }
}

fun KotlinClassElement.validateHasOverlappingFields(
        source: KotlinClassElement,
        targetProperties: Map<String, TypeName>
): Validated<ValidationMessage, Map<String, TypeName>> {
    val sourceProperties = source.classData.propertyList
            .filter { property -> property.setterVisibility in MorphProcessor.ALLOWABLE_PROPERTY_VISIBILITY }
            .associateBy({ source.nameResolver.getString(it.name) },
                    { it.returnType.asTypeName(source.nameResolver, source.classData::getTypeParameter, true) })
    return if (targetProperties
                    .map { (k, v) -> k to v.copy(nullable = false) }
                    .toList().toSet()
                    .intersect(sourceProperties.map { (k, v) -> k to v.copy(nullable = false) }
                            .toList().toSet()).isNotEmpty()) {
        Valid(sourceProperties)
    } else {
        Invalid(listOf(ValidationMessage.NoOverlappingFields(this.element, source.element)))
    }
}

fun MorphTargetsAndSources.validate(): Validated<ValidationMessage, ValidatedContext> {

    // check if target is kotlin class
    val validatedIsKotlinClass = target.validateTargetIsKotlinClass()

    // check if sources is empty list
    val validatedSourcesAreNonEmptyList = sources.validateSourcesAreNotEmpty(target)

    // check if sources are all kotlin classes
    val validatedSources = sources
            .map { it.validateSourceIsKotlinClass(target) }
            .fold( Valid(listOf<KotlinClassElement>()) as Validated<ValidationMessage, List<KotlinClassElement>> ) {
                acu, sourceValidation -> acu.combine(sourceValidation) {acc, elem -> acc + elem}
            }

    return validatedIsKotlinClass
            .combine(validatedSourcesAreNonEmptyList, ::keepLeft)
            .combine(validatedSources) { validatedTarget, validatedSources ->
                KotlinTargetsAndSources(validatedTarget, validatedSources)
            }.combineRight { targetsAndSources ->
                // check if target has primary constructor
                targetsAndSources.target.validatePrimaryConstructor()
                        .combineRight {
                            // check if source and targets have overlapping fields
                            val targetProperties = targetsAndSources.target.classData.propertyList
                                    .filter { property -> property.getterVisibility in MorphProcessor.ALLOWABLE_PROPERTY_VISIBILITY }
                                    .associateBy(
                                            { targetsAndSources.target.nameResolver.getString(it.name) },
                                            { it.returnType.asTypeName(targetsAndSources.target.nameResolver, targetsAndSources.target.classData::getTypeParameter, true) })
                            val validatedOverlappingFields = targetsAndSources.sources
                                    .map { targetsAndSources.target.validateHasOverlappingFields(it, targetProperties) }
                                    .fold(Valid(listOf<Map<String, TypeName>>()) as Validated<ValidationMessage, List<Map<String, TypeName>>>) {
                                        acu, validated -> acu.combine(validated) { acc, elem -> acc + elem }
                                    }
                            (Valid(Unit) as Validated<ValidationMessage, Unit>)
                                    .combine(validatedOverlappingFields) { _, sourcePropertiesList ->
                                ValidatedContext(
                                        ComputationContext(
                                                targetsAndSources.target.element,
                                                targetsAndSources.target.classData,
                                                targetsAndSources.target.nameResolver,
                                                targetProperties
                                        ),
                                        sourcePropertiesList.zip(targetsAndSources.sources) {
                                            sourceProperties, sourceKotlinElement -> ComputationContext(
                                                sourceKotlinElement.element,
                                                sourceKotlinElement.classData,
                                                sourceKotlinElement.nameResolver,
                                                sourceProperties
                                        )
                                        }
                                )
                            }
                        }
            }
}