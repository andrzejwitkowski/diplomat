package com.diplomat;

import com.google.common.base.Joiner;
import java.util.List;

/**
 * Entry point for the diplomat CLI application.
 *
 * <p>Uses Guava's {@link Joiner} so the build exercises real dependency
 * resolution from Maven Central rather than only the JDK.
 */
public class App {

    private static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

    /**
     * Builds a diplomatic greeting addressed to the given delegates.
     *
     * @param delegates the parties to greet
     * @return a formatted greeting line
     */
    public String greet(List<String> delegates) {
        String audience = delegates.isEmpty() ? "esteemed guests" : COMMA_JOINER.join(delegates);
        return "Greetings, " + audience + ". The diplomat is ready to negotiate.";
    }

    public static void main(String[] args) {
        List<String> delegates = List.of(args);
        System.out.println(new App().greet(delegates));
    }
}
