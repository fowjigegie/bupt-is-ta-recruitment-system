package com.bupt.tarecruitment.common.skill;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 提供技能目录、分类建议和相关技能映射。
 */
public final class SkillCatalog {
    private static final Pattern SKILL_VALUE_PATTERN = Pattern.compile("[A-Za-z0-9+#.-]+(?: [A-Za-z0-9+#.-]+)*");

    private static final List<Set<String>> WEAK_MATCH_FAMILIES = List.of(
        skillFamily("programming", "algorithms", "data structures", "object-oriented programming", "java", "python", "c", "c++", "javascript", "typescript"),
        skillFamily("software engineering", "git", "debugging", "linux", "backend development", "rest api", "api design", "software architecture"),
        skillFamily("web development", "html", "css", "javascript", "typescript", "frontend", "react", "vue", "ui design", "ux design", "android", "mobile development", "node.js"),
        skillFamily("machine learning", "deep learning", "pytorch", "tensorflow", "python", "data analysis", "data mining", "big data"),
        skillFamily("database design", "sql", "database", "data modeling", "data analysis", "data visualization", "excel", "statistics", "data engineering", "etl", "big data", "hadoop", "spark"),
        skillFamily("testing", "unit testing", "integration testing", "junit", "test design", "quality assurance", "qa", "debugging"),
        skillFamily("communication", "presentation", "writing", "speaking", "public speaking", "teaching", "tutoring", "mentoring", "teamwork", "patience"),
        skillFamily("networking", "computer networks", "wireshark", "network analysis", "operating systems"),
        skillFamily("security", "cryptography", "network security", "information security", "cloud computing", "aws", "docker", "devops"),
        skillFamily("hardware", "embedded systems", "microcontrollers", "digital logic", "fpga", "computer architecture"),
        skillFamily("analytical thinking", "problem solving", "research", "academic writing", "critical thinking", "literature review", "mathematics", "detail orientation")
    );

    private static final LinkedHashMap<String, LinkedHashSet<String>> BASE_CATEGORIES = buildBaseCategories();
    private static final Map<String, Set<String>> RELATED_SKILLS = buildRelatedSkills();

    private SkillCatalog() {
    }

    public static Map<String, Set<String>> relatedSkills() {
        return RELATED_SKILLS;
    }

    public static List<String> commonSkills() {
        return categorizedSkills().values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public static Map<String, List<String>> categorizedSkills() {
        return toDisplayMap(copyBaseCategories());
    }

    public static List<String> mergeSuggestedSkills(List<String> dynamicSkills) {
        return mergeSuggestedSkillCategories(dynamicSkills).values().stream()
            .flatMap(List::stream)
            .toList();
    }

    public static Map<String, List<String>> mergeSuggestedSkillCategories(List<String> dynamicSkills) {
        LinkedHashMap<String, LinkedHashSet<String>> merged = copyBaseCategories();
        for (String skill : dynamicSkills) {
            String normalized = normalize(skill);
            if (normalized.isBlank()) {
                continue;
            }
            merged.computeIfAbsent(categoryFor(normalized), ignored -> new LinkedHashSet<>())
                .add(normalized);
        }
        return toDisplayMap(merged);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidSkillValue(String value) {
        return value != null && !value.isBlank() && SKILL_VALUE_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean areRelatedSkills(String leftValue, String rightValue) {
        String left = normalize(leftValue);
        String right = normalize(rightValue);
        if (left.equals(right) || left.isBlank() || right.isBlank()) {
            return false;
        }

        Set<String> leftGroup = RELATED_SKILLS.get(left);
        if (leftGroup != null && leftGroup.contains(right)) {
            return true;
        }

        Set<String> rightGroup = RELATED_SKILLS.get(right);
        if (rightGroup != null && rightGroup.contains(left)) {
            return true;
        }

        return sharesMeaningfulToken(left, right) || containsMeaningfulPhrase(left, right);
    }

    private static LinkedHashMap<String, LinkedHashSet<String>> buildBaseCategories() {
        LinkedHashMap<String, LinkedHashSet<String>> categories = new LinkedHashMap<>();
        categories.put("Programming & Software", normalizedSet(
            "Programming", "Algorithms", "Data Structures", "Object-Oriented Programming", "Java", "Python", "C", "C++", "JavaScript", "TypeScript", "Git", "Linux", "Debugging", "Software Engineering", "Backend Development", "REST API", "API Design", "Software Architecture"
        ));
        categories.put("Web, UI & Mobile", normalizedSet(
            "Web Development", "HTML", "CSS", "Frontend", "React", "Vue", "UI Design", "UX Design", "Android", "Mobile Development", "Node.js"
        ));
        categories.put("AI & Machine Learning", normalizedSet(
            "Machine Learning", "Deep Learning", "PyTorch", "TensorFlow", "Data Mining", "Big Data"
        ));
        categories.put("Data & Databases", normalizedSet(
            "Data Analysis", "Statistics", "Excel", "SQL", "Database", "Database Design", "Data Modeling", "Data Visualization", "Data Engineering", "ETL", "Hadoop", "Spark", "Mathematics"
        ));
        categories.put("Testing & Quality", normalizedSet(
            "Testing", "Unit Testing", "Integration Testing", "JUnit", "Test Design", "Quality Assurance", "QA", "Detail Orientation"
        ));
        categories.put("Communication & Teaching", normalizedSet(
            "Communication", "Presentation", "Public Speaking", "Speaking", "Writing", "Teaching", "Tutoring", "Mentoring", "Teamwork", "Patience"
        ));
        categories.put("Networks, Systems & Security", normalizedSet(
            "Networking", "Computer Networks", "Network Analysis", "Wireshark", "Security", "Network Security", "Information Security", "Cryptography", "Operating Systems", "Cloud Computing", "AWS", "Docker", "DevOps"
        ));
        categories.put("Hardware & Embedded", normalizedSet(
            "Hardware", "Embedded Systems", "Digital Logic", "Microcontrollers", "FPGA", "Computer Architecture"
        ));
        categories.put("Analytical & Research", normalizedSet(
            "Analytical Thinking", "Problem Solving", "Research", "Academic Writing", "Critical Thinking", "Literature Review"
        ));
        categories.put("Other Skills", new LinkedHashSet<>());
        return categories;
    }

    private static Map<String, Set<String>> buildRelatedSkills() {
        Map<String, Set<String>> related = new LinkedHashMap<>();
        for (Set<String> family : WEAK_MATCH_FAMILIES) {
            for (String skill : family) {
                LinkedHashSet<String> peers = new LinkedHashSet<>(family);
                peers.remove(skill);
                related.put(skill, Set.copyOf(peers));
            }
        }
        return Map.copyOf(related);
    }

    private static LinkedHashMap<String, LinkedHashSet<String>> copyBaseCategories() {
        LinkedHashMap<String, LinkedHashSet<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : BASE_CATEGORIES.entrySet()) {
            copied.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        return copied;
    }

    private static Map<String, List<String>> toDisplayMap(LinkedHashMap<String, LinkedHashSet<String>> categories) {
        LinkedHashMap<String, List<String>> displayMap = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : categories.entrySet()) {
            displayMap.put(
                entry.getKey(),
                entry.getValue().stream()
                    .map(SkillCatalog::displayLabel)
                    .toList()
            );
        }
        return Collections.unmodifiableMap(displayMap);
    }

    private static String categoryFor(String normalizedSkill) {
        for (Map.Entry<String, LinkedHashSet<String>> entry : BASE_CATEGORIES.entrySet()) {
            if (entry.getValue().contains(normalizedSkill)) {
                return entry.getKey();
            }
        }

        if (containsAny(normalizedSkill, "java", "python", "program", "script", "c++", "algorithm", "git", "linux", "debug", "backend", "api", "architecture")) {
            return "Programming & Software";
        }
        if (containsAny(normalizedSkill, "html", "css", "frontend", "web", "android", "ui", "mobile", "react", "vue", "ux", "node")) {
            return "Web, UI & Mobile";
        }
        if (containsAny(normalizedSkill, "machine learning", "deep learning", "pytorch", "tensorflow", "data mining", "big data")) {
            return "AI & Machine Learning";
        }
        if (containsAny(normalizedSkill, "data", "sql", "database", "statistics", "excel", "model", "visualization", "etl", "hadoop", "spark", "math")) {
            return "Data & Databases";
        }
        if (containsAny(normalizedSkill, "test", "quality", "qa", "junit", "detail")) {
            return "Testing & Quality";
        }
        if (containsAny(normalizedSkill, "communication", "presentation", "teaching", "tutoring", "writing", "speaking", "teamwork", "mentor", "patience")) {
            return "Communication & Teaching";
        }
        if (containsAny(normalizedSkill, "network", "security", "crypt", "system", "wireshark", "cloud", "docker", "aws", "devops")) {
            return "Networks, Systems & Security";
        }
        if (containsAny(normalizedSkill, "hardware", "embedded", "microcontroller", "logic", "architecture", "fpga")) {
            return "Hardware & Embedded";
        }
        if (containsAny(normalizedSkill, "analysis", "analytical", "research", "problem", "critical", "literature")) {
            return "Analytical & Research";
        }
        return "Other Skills";
    }

    private static boolean containsAny(String normalizedSkill, String... keywords) {
        for (String keyword : keywords) {
            if (normalizedSkill.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sharesMeaningfulToken(String left, String right) {
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }

        for (String token : leftTokens) {
            if (token.length() >= 4 && rightTokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMeaningfulPhrase(String left, String right) {
        String shorter = left.length() <= right.length() ? left : right;
        String longer = shorter == left ? right : left;
        return shorter.length() >= 4 && longer.contains(shorter);
    }

    private static Set<String> tokenize(String rawValue) {
        return List.of(rawValue.split("[^a-z0-9+#]+")).stream()
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> skillFamily(String... values) {
        return List.of(values).stream()
            .map(SkillCatalog::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static LinkedHashSet<String> normalizedSet(String... values) {
        return List.of(values).stream()
            .map(SkillCatalog::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String displayLabel(String normalizedSkill) {
        return List.of(normalizedSkill.split(" ")).stream()
            .filter(part -> !part.isBlank())
            .map(part -> part.length() <= 2
                ? part.toUpperCase(Locale.ROOT)
                : Character.toUpperCase(part.charAt(0)) + part.substring(1))
            .collect(Collectors.joining(" "));
    }
}
