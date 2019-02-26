package morph.compiler


import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility
import com.ltrojanowski.morph.api.Morph
import morph.compiler.MorphProcessor.Companion.OPTION_GENERATED
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.lang.model.type.TypeMirror


@SupportedOptions(OPTION_GENERATED)
@AutoService(Processor::class)
class MorphProcessor : AbstractProcessor() {

    companion object {
        /**
         * This annotation processing argument can be specified to have a `@Generated` annotation
         * included in the generated code. It is not encouraged unless you need it for static analysis
         * reasons and not enabled by default.
         *
         * Note that this can only be one of the following values:
         *   * `"javax.annotation.processing.Generated"` (JRE 9+)
         *   * `"javax.annotation.Generated"` (JRE <9)
         */
        const val OPTION_GENERATED = "morph.generated"
        private val POSSIBLE_GENERATED_NAMES = setOf(
                "javax.annotation.processing.Generated",
                "javax.annotation.Generated"
        )
        val ALLOWABLE_PROPERTY_VISIBILITY = setOf(Visibility.INTERNAL, Visibility.PUBLIC)
    }


    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var elements: Elements
    private lateinit var types: Types
    private lateinit var options: Map<String, String>
    private lateinit var outputDir: File
    private var generatedAnnotation: AnnotationSpec? = null


    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        messager = processingEnv.messager
        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        options = processingEnv.options
        outputDir = options["kapt.kotlin.generated"]?.let(::File) ?: throw IllegalStateException(
                "No kapt.kotlin.generated option provided")
        generatedAnnotation = options[OPTION_GENERATED]?.let {
            require(it in POSSIBLE_GENERATED_NAMES) {
                "Invalid option value for $OPTION_GENERATED. Found $it, allowable values are $POSSIBLE_GENERATED_NAMES."
            }
            elements.getTypeElement(it)
        }?.let {
            AnnotationSpec.builder(it.asClassName())
                    .addMember("value = [%S]", MorphProcessor::class.java.canonicalName)
                    .addMember("comments = %S", "https://github.com/ltrojanowski/morph")
                    .build()
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Morph::class.java.name)
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val validatedContexts = roundEnv.getElementsAnnotatedWith(Morph::class.java)
            .map { it as TypeElement }
            .map { MorphTargetsAndSources(
                it,
                it.annotationMirrorByType(Morph::class.java)
                        ?.annotationClassValuesByKey("from")
                        ?.map { it.value as TypeMirror }
                        ?.map { it.asTypeElement(processingEnv) } ?: emptyList()
            ) }
            .map { it.validate() }
            .fold(Valid(listOf<ValidatedContext>()) as Validated<ValidationMessage, List<ValidatedContext>>) {
                acu, validatedContext -> acu.combine(validatedContext) {acc, con -> acc + con}
            }

        when (validatedContexts) {
           is Valid -> validatedContexts.value.forEach {
               it.generateMorphExtensions().writeTo(outputDir)
           }
           is Invalid -> validatedContexts.value.forEach {
               messager.printMessage(
                       it.kind,
                       it.msg,
                       it.element
               )
           }
        }

        return true
    }

}