package com.interviewpilot.application.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic check that AI-written text does not claim more than the resume supports. A text is rejected when
 * it contains a number that is not in the resume (invented metrics or years), names a skill the job requires
 * but the resume does not show (e.g. "AWS" when the match analysis lists AWS as missing), or uses a name the
 * resume does not contain (e.g. an invented employer).
 */
public final class ClaimGuard {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
    /** A capitalised word inside a sentence (after a lower-case letter, digit or comma), i.e. a name, product or acronym. */
    private static final Pattern NAME = Pattern.compile("(?<=[\\p{Ll}\\p{N},;)]\\s)\\p{Lu}[\\p{L}\\p{N}+#]*(?:\\.[\\p{L}\\p{N}]+)*");
    private static final Set<String> COMMON_NAMES = Set.of("january", "february", "march", "april", "may", "june", "july",
            "august", "september", "october", "november", "december", "english");
    private static final Set<String> STOP_WORDS = Set.of("experience", "with", "and", "the", "for", "using", "knowledge",
            "skills", "skill", "understanding", "familiarity", "strong", "good", "basic", "working", "years", "year", "plus",
            "nice", "have", "proficiency", "ability", "tools", "such", "like", "etc", "of", "in", "on", "to", "a", "an", "or",
            "based", "build", "building", "design", "designing", "development", "developing", "solutions", "systems",
            "services", "environment", "environments", "practices", "concepts", "principles", "hands");

    private final String sourceText;
    private final String namesText;
    private final List<String> forbiddenTerms;
    private final List<String> allowedPhrases;

    public ClaimGuard(String sourceText, Collection<String> missingSkills) {
        this(sourceText, missingSkills, List.of(), "");
    }

    /**
     * @param sourceText    lower-case text of the resume
     * @param missingSkills skills or requirements the match analysis says the resume lacks; any of their words
     *                      that do not occur in the resume become forbidden
     * @param allowedPhrases  phrases that may always be used, e.g. the job title and company name in a cover letter
     * @param otherNamesText more text whose names may be mentioned (not claimed as skills), e.g. the job description
     */
    public ClaimGuard(String sourceText, Collection<String> missingSkills, Collection<String> allowedPhrases, String otherNamesText) {
        this.sourceText = sourceText.toLowerCase(Locale.ROOT);
        this.namesText = this.sourceText + "\n" + otherNamesText.toLowerCase(Locale.ROOT);
        this.allowedPhrases = allowedPhrases.stream().filter(phrase -> phrase != null && !phrase.isBlank())
                .map(phrase -> phrase.trim().toLowerCase(Locale.ROOT)).toList();
        Set<String> terms = new LinkedHashSet<>();
        for (String missing : missingSkills) {
            String phrase = missing.trim().toLowerCase(Locale.ROOT);
            if (!phrase.isEmpty() && !containsTerm(this.sourceText, phrase)) terms.add(phrase);
            for (String word : phrase.split("[^\\p{L}\\p{N}+#.]+")) {
                String term = word.replaceAll("^\\.+|\\.+$", "");
                if (term.length() >= 2 && !STOP_WORDS.contains(term) && !containsTerm(this.sourceText, term)) terms.add(term);
            }
        }
        this.forbiddenTerms = new ArrayList<>(terms);
    }

    public List<String> forbiddenTerms() {
        return forbiddenTerms;
    }

    public boolean isSupported(String text) {
        if (text == null || text.isBlank()) return false;
        String checked = text;
        for (String phrase : allowedPhrases) {
            checked = Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(checked).replaceAll(" ");
        }
        Matcher numbers = NUMBER.matcher(checked);
        while (numbers.find()) {
            if (!sourceHasNumber(numbers.group())) return false;
        }
        Matcher names = NAME.matcher(checked);
        while (names.find()) {
            String name = names.group().toLowerCase(Locale.ROOT);
            if (name.length() > 1 && !COMMON_NAMES.contains(name) && !containsTerm(namesText, name)) return false;
        }
        String lower = checked.toLowerCase(Locale.ROOT);
        return forbiddenTerms.stream().noneMatch(term -> containsTerm(lower, term));
    }

    /** Whole numbers only: an invented "20%" must not pass because the resume mentions "2020". */
    private boolean sourceHasNumber(String number) {
        return Pattern.compile("(?<![\\d.,])" + Pattern.quote(number) + "(?!\\d|[.,]\\d)").matcher(sourceText).find();
    }

    /** Keeps only the supported sentences of a paragraph (empty when none are). */
    public String keepSupportedSentences(String paragraph) {
        List<String> kept = new ArrayList<>();
        for (String sentence : SENTENCE_END.split(paragraph == null ? "" : paragraph.trim())) {
            if (isSupported(sentence)) kept.add(sentence.trim());
        }
        return String.join(" ", kept);
    }

    /** Whole-word, case-insensitive containment; works for terms like "c#", "node.js" or "spring boot". */
    public static boolean containsTerm(String lowerText, String lowerTerm) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(lowerTerm) + "(?![\\p{L}\\p{N}])").matcher(lowerText).find();
    }
}
