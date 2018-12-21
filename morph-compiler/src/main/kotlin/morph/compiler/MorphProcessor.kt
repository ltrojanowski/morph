package morph.compiler


import arrow.core.orNull
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility
import morph.api.Morph
import morph.api.MorphBuilder
import morph.compiler.MorphProcessor.Companion.OPTION_GENERATED
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import me.eugeniomarletti.kotlin.metadata.isPrimary
import me.eugeniomarletti.kotlin.metadata.visibility
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.resolve.constants.KClassValue


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

        val maybeContext = roundEnv.getElementsAnnotatedWith(Morph::class.java)
            .map { it as TypeElement }
            .map { MorphTargetsAndSources(
                it,
                it.getAnnotation(Morph::class.java).from.map { it as TypeElement }
            ) }
            .map { it.validate() }

        val messageList = maybeContext
                .filter { it.isLeft() }
                .flatMap { it.swap().orNull()!! }
        if (messageList.isNotEmpty()) {
            messageList.forEach {
                messager.printMessage(
                        it.kind,
                        it.msg,
                        it.element
                )
            }
        } else {
            maybeContext
                    .filter { it.isRight() }
                    .map { it.orNull()!! }
                    .forEach { it.generateMorphExtensions() }
        }

        return true
    }


    private fun codeGenerator(context: ValidatedContext, pack: String) {

        val file = FileSpec.builder(pack, "${context.target.element.asClassName().simpleName}MorphExtensions")
        val targetClassTypeParmeter = context.target.element.asType()

        context.sources.forEach { source ->
            val extFunction = generateExtensionFunctionFor(context.target, source)
        }
    }

    private fun generateBuilder(target: KotlinClassElement) {
        val targetConstructor = target.classData.constructorList.find { it.isPrimary }
        val paramaterList = targetConstructor!!.valueParameterList
        val typeParams = target.classData.typeParameterList.map { it.asTypeName(nameResolver)}
        val parameterSpecList = paramaterList.map {
            ParameterSpec.builder("foo", typeParams[it.name].asTypeName, KModifier.PUBLIC)
        }
//        val targetProperties = target.classData.constructorList.find { it.isPrimary }.propertyList
//                .filter { it.visibility !in MorphProcessor.ALLOWABLE_PROPERTY_VISIBILITY}
//                .map {
//                    Pair(target.classData.getTypeParameter(it.name), target.nameResolver.getString(it.name))
//                }.toSet()
        val builderConstructor = FunSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder())
        TypeSpec
                .classBuilder(target.element.asClassName())
                .addSuperinterface(MorphBuilder::class.parameterizedBy(target.element::class))
                .primaryConstructor()
                .addFunction(
                        FunSpec.builder("morph")
                                .addModifiers(KModifier.OVERRIDE)
                                .addStatement("")
                )
    }

    private fun generateExtensionFunctionFor(target: KotlinClassElement, source: KotlinClassElement) {
        FunSpec.builder("morph")
    }

}