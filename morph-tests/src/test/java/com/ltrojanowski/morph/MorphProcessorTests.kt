package com.ltrojanowski.morph

import morph.compiler.MorphProcessor

class MorphProcessorTests : APTest("com.ltrojanowski.morph") {

    init {

        /*
        * Morph usage validation tests
        * */

        /* TODO: test this directly without using stubs
        testProcessor(AnnotationProcessor(
                name = "Morph cannot be used on java classes",
                sourceFiles = listOf("NotKotlinClass.java"),
                errorMessages = "@Morph can't be applied to com.ltrojanowski.morph.NotKotlinClass: must be a Kotlin class",
                processor = MorphProcessor()
        ))*/

        testProcessor(AnnotationProcessor(
                name = "Morph has to have at least one source",
                sourceFiles = listOf("TargetHasNoSources.java"),
                errorMessage = "@Morph applied to com.ltrojanowski.morph.TargetHasNoSources: has empty parameter 'from'",
                processor = MorphProcessor()
        ))

        testProcessor(AnnotationProcessor(
                name = "Sources must be kotlin classes",
                sourceFiles = listOf("TargetWithJavaSources.java"),
                errorMessage = "@Morph applied to com.ltrojanowski.morph.TargetWithJavaSources has invalid members of parameter 'from': com.ltrojanowski.morph.NotKotlinClass is not a kotlin class",
                processor = MorphProcessor()
        ))

        testProcessor(AnnotationProcessor(
                name = "Target must have a primary constructor",
                sourceFiles = listOf("TargetHasNoPrimaryConstructor.java"),
                errorMessage = "@Morph can't be applied to com.ltrojanowski.morph.TargetHasNoPrimaryConstructor: must have a primary constructor",
                processor = MorphProcessor()
        ))

        testProcessor(AnnotationProcessor(
                name = "Target must have overlapping fields with source - no matching name",
                sourceFiles = listOf("TargetHasNoOverlappingNames.java"),
                errorMessage = "@Morph applied to com.ltrojanowski.morph.TargetHasNoOverlappingNames has invalid parameter 'from': no fields overlap by name and type for com.ltrojanowski.morph.SourceForTargetWithNoOverlappingNames",
                processor = MorphProcessor()
        ))

        testProcessor(AnnotationProcessor(
                name = "Target must have overlapping fields with source - no matching type",
                sourceFiles = listOf("TargetHasNoOverlappingTypes.java"),
                errorMessage = "@Morph applied to com.ltrojanowski.morph.TargetHasNoOverlappingTypes has invalid parameter 'from': no fields overlap by name and type for com.ltrojanowski.morph.SourceForTargetWithNoOverlappingTypes",
                processor = MorphProcessor()
        ))

        testProcessor(AnnotationProcessor(
                name = "Target must have overlapping fields with source - no matching nullability",
                sourceFiles = listOf("TargetHasNoOverlappingNullability.java"),
                errorMessage = "@Morph applied to com.ltrojanowski.morph.TargetHasNoOverlappingNullability has invalid parameter 'from': no fields overlap by name and type for com.ltrojanowski.morph.SourceForTargetWithNoOverlappingNullability",
                processor = MorphProcessor()
        ))

        /*
        * Code generation test
        * */

        testProcessor(AnnotationProcessor(
                name = "Code generation for perfect match",
                sourceFiles = listOf("TargetPerfectMatch.java"),
                destFile = "TargetPerfectMatchMorphExtensions.kt",
                processor = MorphProcessor()
        ))

    }
}