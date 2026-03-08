package com.yusheng.aiagentproject.advisor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForbiddenWordsAdvisorTest {

    @Test
    void sanitizeText_replacesCaseInsensitiveMatches() {
        String input = "This is Bad and BAD!";
        String output = ForbiddenWordsAdvisor.sanitizeText(input, List.of("bad"));
        assertEquals("This is *** and ***!", output);
    }

    @Test
    void sanitizeText_replacesSubstringMatches() {
        String input = "badwording";
        String output = ForbiddenWordsAdvisor.sanitizeText(input, List.of("badword"));
        assertEquals("*******ing", output);
    }

    @Test
    void sanitizeText_handlesEmptyList() {
        String input = "nothing changes";
        String output = ForbiddenWordsAdvisor.sanitizeText(input, List.of());
        assertEquals(input, output);
    }
}
