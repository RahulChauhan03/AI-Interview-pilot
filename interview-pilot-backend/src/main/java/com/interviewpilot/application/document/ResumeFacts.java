package com.interviewpilot.application.document;

import com.interviewpilot.resume.document.ParsedResume;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The verified facts of a parsed resume, read from both the current and the older free-form parsing format.
 * Everything the AI writes is checked against these facts.
 */
public record ResumeFacts(
        String name,
        List<String> contact,
        String summary,
        List<String> skills,
        List<Job> experience,
        List<ProjectFact> projects,
        List<TailoredResumeContent.Education> education,
        List<String> certifications,
        /** Lower-case text of the whole resume, used to verify claims. */
        String sourceText) {

    public record Job(String title, String company, String duration, String location, List<String> bullets, List<String> technologies) {
    }

    public record ProjectFact(String name, String description, List<String> technologies) {
    }

    /** Values the AI writes for missing data. */
    private static final Pattern PLACEHOLDER = Pattern.compile("(?i)^(none|n/?a|null|undefined|-+|not (available|specified|provided|mentioned))$");

    public static ResumeFacts from(ParsedResume parsed, String fallbackName) {
        Map<String, Object> info = parsed.getPersonalInformation() == null ? Map.of() : parsed.getPersonalInformation();
        String name = join(" ", text(info, "firstName", "First Name"), text(info, "lastName", "Last Name"));
        List<String> contact = new ArrayList<>();
        for (String[] keys : new String[][] {{"email", "Email"}, {"phone", "Phone"}, {"location", "Location"},
                {"linkedIn", "LinkedIn", "linkedin"}, {"github", "Github", "GitHub"}, {"portfolio", "Portfolio"}}) {
            String value = text(info, keys);
            if (!value.isEmpty()) contact.add(value);
        }

        Map<String, String> skills = new LinkedHashMap<>(); // keeps the first spelling, de-duplicates case-insensitively
        addAll(skills, parsed.getTechnicalSkills());
        addAll(skills, parsed.getSkills());

        List<Job> jobs = new ArrayList<>();
        for (Map<String, Object> entry : maps(parsed.getExperience())) {
            Job job = new Job(text(entry, "designation", "title", "role"), text(entry, "company", "companyName", "organization"),
                    text(entry, "duration", "dates", "employmentDates"), text(entry, "location", "city"),
                    list(entry, "responsibilities", "description"), list(entry, "technologies", "techStack", "skills"));
            if (!job.company().isEmpty() || !job.title().isEmpty()) {
                jobs.add(job);
                addAll(skills, job.technologies());
            }
        }
        List<ProjectFact> projects = new ArrayList<>();
        for (Map<String, Object> entry : maps(parsed.getProjects())) {
            ProjectFact project = new ProjectFact(text(entry, "name", "projectName", "title"), text(entry, "description"),
                    list(entry, "technologies", "techStack"));
            if (!project.name().isEmpty()) {
                projects.add(project);
                addAll(skills, project.technologies());
            }
        }
        List<TailoredResumeContent.Education> education = new ArrayList<>();
        for (Map<String, Object> entry : maps(parsed.getEducation())) {
            String degree = join(", ", text(entry, "degree"), text(entry, "fieldOfStudy"));
            String institution = text(entry, "institution", "university", "school");
            String grade = text(entry, "grade");
            if (!degree.isEmpty() || !institution.isEmpty()) {
                education.add(new TailoredResumeContent.Education(degree, institution, text(entry, "duration"),
                        grade.isEmpty() ? "" : "Grade: " + grade));
            }
        }
        List<String> certifications = clean(parsed.getCertifications());
        String source = String.join("\n", nonNull(parsed.getCleanText()), nonNull(parsed.getRawText()), String.join(" ", skills.values()))
                .toLowerCase(Locale.ROOT);
        return new ResumeFacts(name.isEmpty() ? fallbackName : name, contact, clean(nonNull(parsed.getSummary())),
                new ArrayList<>(skills.values()), jobs, projects, education, certifications, source);
    }

    /** True when the resume contains this skill (as a listed skill or anywhere in its text). */
    public boolean hasSkill(String skill) {
        String key = skill.trim().toLowerCase(Locale.ROOT);
        return skills.stream().anyMatch(own -> own.toLowerCase(Locale.ROOT).equals(key))
                || ClaimGuard.containsTerm(sourceText, key);
    }

    private static String text(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String string || value instanceof Number) {
                String cleaned = clean(String.valueOf(value));
                if (!cleaned.isEmpty()) return cleaned;
            }
        }
        return "";
    }

    private static List<String> list(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List<?> items) {
                return clean(items.stream().map(String::valueOf).toList());
            }
            if (value instanceof String string && !clean(string).isEmpty()) {
                return List.of(clean(string));
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(List<Map<String, Object>> entries) {
        return entries == null ? List.of() : entries.stream().filter(Map.class::isInstance).map(entry -> (Map<String, Object>) entry).toList();
    }

    private static void addAll(Map<String, String> target, List<String> values) {
        for (String value : clean(values)) {
            target.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
        }
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of() : values.stream().map(ResumeFacts::clean).filter(value -> !value.isEmpty()).distinct().toList();
    }

    private static String clean(String value) {
        String trimmed = value == null ? "" : value.trim();
        return PLACEHOLDER.matcher(trimmed).matches() ? "" : trimmed;
    }

    private static String join(String separator, String... parts) {
        return String.join(separator, java.util.Arrays.stream(parts).filter(part -> !part.isEmpty()).toList());
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
