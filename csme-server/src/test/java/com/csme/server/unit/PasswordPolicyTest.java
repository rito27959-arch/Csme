package com.csme.server.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.server.domain.rules.PasswordPolicy;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsReasonablePassword() {
        assertThat(PasswordPolicy.violation("Tropical-Fruit-42", "marie@acme.cm")).isEmpty();
    }

    @Test
    void rejectsShortMissingDigitOrLetterAndEmailBased() {
        assertThat(PasswordPolicy.violation("abc12", null)).isPresent();
        assertThat(PasswordPolicy.violation("onlyletterspassword", null)).isPresent();
        assertThat(PasswordPolicy.violation("1234567890123", null)).isPresent();
        assertThat(PasswordPolicy.violation("marie2026secure", "marie@acme.cm")).isPresent();
        assertThat(PasswordPolicy.violation(null, null)).isPresent();
    }
}
