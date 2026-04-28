package com.pullwise.api.application.service.graph.blast;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityKeywordMatcherTest {

    private final SecurityKeywordMatcher matcher = new SecurityKeywordMatcher();

    @Test
    void noMatch_returnsZero() {
        assertThat(matcher.score("UserService", "com.example.UserService")).isZero();
        assertThat(matcher.hasMatch("Foo", "com.bar.Foo")).isFalse();
    }

    @Test
    void singleMatch_returnsHalf() {
        assertThat(matcher.score("AuthFilter", "com.x.AuthFilter")).isEqualTo(0.5);
        assertThat(matcher.score("PasswordReset", "com.x.PasswordReset")).isEqualTo(0.5);
    }

    @Test
    void multipleMatches_returnsOne() {
        // "auth" + "token"
        assertThat(matcher.score("AuthTokenService", "com.x.AuthTokenService")).isEqualTo(1.0);
        // "encrypt" + "verify"
        assertThat(matcher.score("EncryptAndVerifySigner", null)).isEqualTo(1.0);
    }

    @Test
    void caseInsensitive() {
        assertThat(matcher.hasMatch("PASSWORDmanager", null)).isTrue();
        assertThat(matcher.hasMatch("OauthProvider", null)).isTrue();
    }

    @Test
    void matchesInQualifiedName() {
        assertThat(matcher.hasMatch("Helper", "com.security.crypto.AesHelper")).isTrue();
    }

    @Test
    void nullInputs_safe() {
        assertThat(matcher.score(null, null)).isZero();
        assertThat(matcher.matches(null, null)).isEmpty();
    }
}
