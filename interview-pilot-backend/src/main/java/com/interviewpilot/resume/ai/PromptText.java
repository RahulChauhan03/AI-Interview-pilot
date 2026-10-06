package com.interviewpilot.resume.ai;

/** Keeps user-provided text inside the model's context window. */
public final class PromptText {

    private PromptText() {
    }

    public static String limit(String text, int maxCharacters) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxCharacters ? text : text.substring(0, maxCharacters);
    }
}
