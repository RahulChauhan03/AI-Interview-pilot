package com.interviewpilot.application;

import com.interviewpilot.resume.document.ParsedResume;
import java.util.List;
import java.util.Map;

/** A small, realistic parsed resume shared by the application tests. */
public final class ApplicationTestData {

    public static final String RESUME_TEXT = """
            Asha Rao | asha@example.com | +91 98765 43210 | Pune
            Backend developer with 3 years of experience in Java and Spring Boot.
            Software Engineer, Infosys, Jun 2021 - Present, Pune
            - Built REST APIs with Spring Boot and MySQL for an insurance claims platform
            - Reduced report generation time by 30% using caching
            - Wrote unit tests with JUnit and Mockito
            Interview Pilot: AI interview practice app built with Angular and Spring Boot
            B.E. Computer Engineering, Pune University, 2017 - 2021, 8.1 CGPA
            Oracle Certified Java Programmer
            """;

    private ApplicationTestData() {
    }

    public static ParsedResume parsedResume() {
        return ParsedResume.builder()
                .resumeId(5L)
                .rawText(RESUME_TEXT)
                .cleanText(RESUME_TEXT)
                .personalInformation(Map.of("firstName", "Asha", "lastName", "Rao", "email", "asha@example.com",
                        "phone", "+91 98765 43210", "location", "Pune", "github", "N/A"))
                .summary("Backend developer with 3 years of experience in Java and Spring Boot.")
                .technicalSkills(List.of("Java", "Spring Boot", "MySQL", "Angular", "JUnit"))
                .skills(List.of("java", "Mockito", "None"))
                .experience(List.of(Map.of(
                        "designation", "Software Engineer",
                        "company", "Infosys",
                        "duration", "Jun 2021 - Present",
                        "location", "Pune",
                        "responsibilities", List.of(
                                "Built REST APIs with Spring Boot and MySQL for an insurance claims platform",
                                "Reduced report generation time by 30% using caching",
                                "Wrote unit tests with JUnit and Mockito"),
                        "technologies", List.of("Java", "Spring Boot", "MySQL"))))
                .projects(List.of(Map.of(
                        "name", "Interview Pilot",
                        "description", "AI interview practice app built with Angular and Spring Boot",
                        "technologies", List.of("Angular", "Spring Boot"))))
                .education(List.of(Map.of(
                        "degree", "B.E.", "fieldOfStudy", "Computer Engineering", "institution", "Pune University",
                        "duration", "2017 - 2021", "grade", "8.1 CGPA")))
                .certifications(List.of("Oracle Certified Java Programmer"))
                .build();
    }
}
