package com.pullwise.api.application.service.graph.blast;

import com.pullwise.api.domain.enums.ConfidenceTier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeConfidenceClassifierTest {

    private final EdgeConfidenceClassifier classifier = new EdgeConfidenceClassifier();

    @Test
    void qualifiedNameInternal_isExtracted() {
        assertThat(classifier.classify("com.example.Foo", "com.example.Foo"))
                .isEqualTo(ConfidenceTier.EXTRACTED);
    }

    @Test
    void simpleName_resolvedInPackage_isInferred() {
        assertThat(classifier.classify("Foo", "com.example.Foo"))
                .isEqualTo(ConfidenceTier.INFERRED);
    }

    @Test
    void simpleName_unresolved_isAmbiguous() {
        assertThat(classifier.classify("Foo", "Foo"))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void genericTypes_areAmbiguous() {
        assertThat(classifier.classify("List<String>", "List<String>"))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("Map<String,Object>", "Map<String,Object>"))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("? extends Foo", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("String[]", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void primitives_areAmbiguous() {
        assertThat(classifier.classify("int", null)).isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("void", null)).isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void externalPackages_areAmbiguous() {
        assertThat(classifier.classify("java.util.Optional", "java.util.Optional"))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("org.springframework.stereotype.Service", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("com.fasterxml.jackson.databind.ObjectMapper", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void blankOrNull_isAmbiguous() {
        assertThat(classifier.classify(null, "x")).isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("", "x")).isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classify("   ", "x")).isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void methodCallOnLowerCaseField_isAmbiguous() {
        assertThat(classifier.classifyMethodCall("user.name", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
        assertThat(classifier.classifyMethodCall("repository.findById", null))
                .isEqualTo(ConfidenceTier.AMBIGUOUS);
    }

    @Test
    void methodCallOnTypeName_classifiesAsType() {
        assertThat(classifier.classifyMethodCall("UserService", "com.example.UserService"))
                .isEqualTo(ConfidenceTier.INFERRED);
    }
}
