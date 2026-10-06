package com.interviewpilot.application.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewpilot.application.ApplicationTestData;
import com.interviewpilot.resume.document.ParsedResume;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResumeFactsTest {

    @Test
    void readsTheCurrentParsingFormat() {
        ResumeFacts facts = ResumeFacts.from(ApplicationTestData.parsedResume(), "Account Name");

        assertThat(facts.name()).isEqualTo("Asha Rao");
        assertThat(facts.contact()).containsExactly("asha@example.com", "+91 98765 43210", "Pune"); // "N/A" github skipped
        assertThat(facts.skills()).containsExactly("Java", "Spring Boot", "MySQL", "Angular", "JUnit", "Mockito");
        assertThat(facts.experience()).singleElement().satisfies(job -> {
            assertThat(job.title()).isEqualTo("Software Engineer");
            assertThat(job.company()).isEqualTo("Infosys");
            assertThat(job.duration()).isEqualTo("Jun 2021 - Present");
            assertThat(job.bullets()).hasSize(3);
        });
        assertThat(facts.projects()).singleElement().satisfies(project -> assertThat(project.name()).isEqualTo("Interview Pilot"));
        assertThat(facts.education()).singleElement().satisfies(education -> {
            assertThat(education.degree()).isEqualTo("B.E., Computer Engineering");
            assertThat(education.details()).isEqualTo("Grade: 8.1 CGPA");
        });
        assertThat(facts.certifications()).containsExactly("Oracle Certified Java Programmer");
    }

    @Test
    void readsTheOlderFreeFormFormatAndFallsBackToTheAccountName() {
        ParsedResume parsed = ParsedResume.builder()
                .cleanText("Developer at Wipro working on Python scripts")
                .personalInformation(Map.of("Email", "dev@example.com"))
                .skills(List.of("Python", "none"))
                .experience(List.of(Map.of("title", "Developer", "companyName", "Wipro", "description", "Python scripts")))
                .projects(List.of(Map.of("projectName", "")))
                .build();

        ResumeFacts facts = ResumeFacts.from(parsed, "Account Name");

        assertThat(facts.name()).isEqualTo("Account Name");
        assertThat(facts.contact()).containsExactly("dev@example.com");
        assertThat(facts.skills()).containsExactly("Python");
        assertThat(facts.experience()).singleElement().satisfies(job -> {
            assertThat(job.company()).isEqualTo("Wipro");
            assertThat(job.bullets()).containsExactly("Python scripts");
        });
        assertThat(facts.projects()).isEmpty();
        assertThat(facts.summary()).isEmpty();
    }

    @Test
    void hasSkillChecksTheSkillListAndTheResumeText() {
        ResumeFacts facts = ResumeFacts.from(ApplicationTestData.parsedResume(), "");

        assertThat(facts.hasSkill("spring boot")).isTrue();
        assertThat(facts.hasSkill("REST APIs")).isTrue();
        assertThat(facts.hasSkill("Kubernetes")).isFalse();
    }
}
