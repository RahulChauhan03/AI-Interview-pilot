package com.interviewpilot.resume.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewpilot.exception.InvalidAiResponseException;
import org.junit.jupiter.api.Test;

public class AiResponseParserTest {

    /** A complete answer in the shape required by resume-analysis-schema.json. */
    public static final String VALID_RESPONSE = """
            {
              "personalInformation": {"firstName": "Jane", "lastName": "Example", "email": "jane@example.com",
                                      "phone": "+1 555 0100", "location": "Pune", "linkedIn": "", "github": "", "portfolio": ""},
              "summary": "Backend engineer focused on Java services.",
              "yearsOfExperience": "3 years",
              "language": "English",
              "experience": [{"company": "Acme", "designation": "Engineer", "duration": "2022 - 2025", "location": "Pune",
                              "responsibilities": ["Built APIs"], "technologies": ["Java", "Spring Boot"]}],
              "education": [{"institution": "Example University", "degree": "B.Tech", "fieldOfStudy": "CS",
                             "duration": "2018 - 2022", "grade": "8.1"}],
              "projects": [{"name": "Interview Pilot", "description": "Mock interviews", "technologies": ["Angular"]}],
              "skills": ["Java", "Communication"],
              "technicalSkills": ["Java", "Spring Boot"],
              "softSkills": ["Communication"],
              "certifications": [],
              "achievements": [],
              "languages": ["English"],
              "companies": ["Acme"],
              "designations": ["Engineer"],
              "keywords": ["Java", "REST"]
            }
            """;

    // Configured like Spring Boot's ObjectMapper, which ignores unknown properties.
    private final AiResponseParser parser = new AiResponseParser(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

    @Test
    void parsesValidResponse() {
        ResumeAnalysis analysis = parser.parse(VALID_RESPONSE, ResumeAnalysis.class);

        assertThat(analysis.personalInformation()).containsEntry("firstName", "Jane");
        assertThat(analysis.technicalSkills()).containsExactly("Java", "Spring Boot");
        assertThat(analysis.experience()).hasSize(1);
        assertThat(analysis.experience().get(0)).containsEntry("company", "Acme");
        assertThat(analysis.language()).isEqualTo("English");
    }

    @Test
    void rejectsInvalidJsonWithoutQuotingIt() {
        assertThatThrownBy(() -> parser.parse("{\"summary\": Jane Example worked at Acme", ResumeAnalysis.class))
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageStartingWith("AI response does not match the expected format")
                .hasMessageNotContaining("Jane")
                .hasNoCause();
    }

    @Test
    void rejectsMissingRequiredField() {
        String withoutSkills = VALID_RESPONSE.replace("\"skills\": [\"Java\", \"Communication\"],", "");

        assertThatThrownBy(() -> parser.parse(withoutSkills, ResumeAnalysis.class))
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("skills");
    }

    @Test
    void rejectsNullField() {
        String nullSummary = VALID_RESPONSE.replace("\"Backend engineer focused on Java services.\"", "null");

        assertThatThrownBy(() -> parser.parse(nullSummary, ResumeAnalysis.class)).isInstanceOf(InvalidAiResponseException.class);
    }

    @Test
    void rejectsWrongFieldType() {
        String skillsAsString = VALID_RESPONSE.replace("\"skills\": [\"Java\", \"Communication\"]", "\"skills\": \"Java, Communication\"");

        assertThatThrownBy(() -> parser.parse(skillsAsString, ResumeAnalysis.class))
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("field: skills")
                .hasMessageNotContaining("Communication");
    }

    @Test
    void rejectsOldFreeFormStructure() {
        assertThatThrownBy(() -> parser.parse("{\"Personal Information\": {\"First Name\": \"Jane\"}, \"Skills\": []}", ResumeAnalysis.class))
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageNotContaining("Jane");
    }

    @Test
    void rejectsEmptyResponse() {
        assertThatThrownBy(() -> parser.parse(" ", ResumeAnalysis.class)).isInstanceOf(InvalidAiResponseException.class);
    }
}
