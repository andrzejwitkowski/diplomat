package com.diplomat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AppTest {

    private final App app = new App();

    @Test
    void greetsNamedDelegates() {
        String greeting = app.greet(List.of("France", "Spain"));
        assertEquals(
                "Greetings, France, Spain. The diplomat is ready to negotiate.", greeting);
    }

    @Test
    void greetsGenericAudienceWhenNoDelegates() {
        String greeting = app.greet(List.of());
        assertTrue(greeting.contains("esteemed guests"), "should fall back to a generic audience");
    }
}
