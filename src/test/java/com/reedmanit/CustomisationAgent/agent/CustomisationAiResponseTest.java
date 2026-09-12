package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomisationAiResponseTest {

    @Test
    void shouldFormatFullResponseToHumanReadableString() {
        CustomisationAiResponse response = new CustomisationAiResponse(
                "Urgent task feature implemented across DB, API, and UI.",
                List.of("Storage Layer: Added urgent column", "UI Layer: Added checkbox", "Verification: Tests passed"),
                "SUCCESS"
        );

        String result = response.toHumanReadableString();

        assertThat(result).isEqualTo("""
                Status: SUCCESS
                Summary: Urgent task feature implemented across DB, API, and UI.
                Steps:
                - Storage Layer: Added urgent column
                - UI Layer: Added checkbox
                - Verification: Tests passed""".trim());
    }

    @Test
    void shouldFormatResponseWithMissingOptionalFields() {
        CustomisationAiResponse response = new CustomisationAiResponse(
                "Customisation complete.",
                null,
                null
        );

        String result = response.toHumanReadableString();

        assertThat(result).isEqualTo("Summary: Customisation complete.");
    }

    @Test
    void shouldReturnEmptyStringWhenAllFieldsAreNullOrBlank() {
        CustomisationAiResponse response = new CustomisationAiResponse(
                "",
                List.of(),
                "   "
        );

        String result = response.toHumanReadableString();

        assertThat(result).isEmpty();
    }
}
