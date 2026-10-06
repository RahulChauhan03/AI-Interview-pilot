package com.interviewpilot.application.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewpilot.application.ApplicationTestData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClaimGuardTest {

    private final String source = ApplicationTestData.RESUME_TEXT.toLowerCase();

    @Test
    void acceptsTextThatOnlyUsesResumeFacts() {
        ClaimGuard guard = new ClaimGuard(source, List.of("AWS", "Kafka"));

        assertThat(guard.isSupported("Built REST APIs with Spring Boot and MySQL")).isTrue();
        assertThat(guard.isSupported("Cut report generation time by 30% with caching")).isTrue();
        assertThat(guard.isSupported("Engineer at Infosys since 2021")).isTrue();
    }

    @Test
    void rejectsNumbersThatAreNotInTheResume() {
        ClaimGuard guard = new ClaimGuard(source, List.of());

        assertThat(guard.isSupported("Improved throughput by 45%")).isFalse();
        assertThat(guard.isSupported("Led a team of 12 engineers")).isFalse();
        // "20" only appears inside "2017" / "2021": not a whole-number match.
        assertThat(guard.isSupported("Reduced costs by 20%")).isFalse();
        assertThat(guard.isSupported("5 years of experience")).isFalse();
    }

    @Test
    void rejectsSkillsTheMatchAnalysisSaysAreMissing() {
        ClaimGuard guard = new ClaimGuard(source, List.of("AWS", "Experience with Kubernetes", "Spring Security"));

        assertThat(guard.isSupported("Deployed services on AWS")).isFalse();
        assertThat(guard.isSupported("Managed kubernetes clusters")).isFalse();
        assertThat(guard.isSupported("Secured APIs with Spring Security")).isFalse();
        // Words of a missing requirement that the resume does contain stay usable.
        assertThat(guard.isSupported("Experience with Spring Boot")).isTrue();
        assertThat(guard.forbiddenTerms()).contains("aws", "kubernetes", "security").doesNotContain("experience", "spring", "with");
    }

    @Test
    void rejectsNamesThatAreNotInTheResume() {
        ClaimGuard guard = new ClaimGuard(source, List.of());

        assertThat(guard.isSupported("Built data pipelines at Google")).isFalse();
        assertThat(guard.isSupported("Worked closely with the Salesforce team")).isFalse();
        assertThat(guard.isSupported("Built REST APIs at Infosys with Spring Boot")).isTrue();
        // Sentence-initial words, "I" and month names are not treated as claims.
        assertThat(guard.isSupported("Wrote unit tests, and I joined Infosys in June 2021")).isTrue();
    }

    @Test
    void allowedPhrasesAndOtherNamesMayBeUsed() {
        ClaimGuard guard = new ClaimGuard(source, List.of("AWS", "Kafka"), List.of("AWS Backend Engineer", "Acme 360"),
                "Join the Payments team. Kafka is a plus.");

        assertThat(guard.isSupported("I am applying for the AWS Backend Engineer role at Acme 360.")).isTrue();
        assertThat(guard.isSupported("I would like to join the Payments team.")).isTrue();
        assertThat(guard.isSupported("I have used AWS daily.")).isFalse();
        // A name from the job description is still a forbidden claim when it is a missing skill.
        assertThat(guard.isSupported("I built pipelines with Kafka.")).isFalse();
    }

    @Test
    void keepsOnlySupportedSentences() {
        ClaimGuard guard = new ClaimGuard(source, List.of("AWS"));

        String kept = guard.keepSupportedSentences("I built REST APIs at Infosys. I also ran AWS workloads! I wrote tests with JUnit.");

        assertThat(kept).isEqualTo("I built REST APIs at Infosys. I wrote tests with JUnit.");
        assertThat(guard.isSupported("  ")).isFalse();
    }

    @Test
    void containsTermMatchesWholeTermsOnly() {
        assertThat(ClaimGuard.containsTerm("i know javascript", "java")).isFalse();
        assertThat(ClaimGuard.containsTerm("c# and .net", "c#")).isTrue();
        assertThat(ClaimGuard.containsTerm("node.js services", "node.js")).isTrue();
        assertThat(ClaimGuard.containsTerm("spring boot apps", "spring boot")).isTrue();
    }
}
