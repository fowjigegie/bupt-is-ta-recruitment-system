package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.common.skill.SkillCatalog;

import java.time.LocalTime;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校验申请人画像字段是否满足业务规则。
 */
public final class ApplicantProfileValidator {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("\\d{8,12}");
    private static final Pattern LETTERS_AND_SPACES_PATTERN = Pattern.compile("[A-Za-z]+(?: [A-Za-z]+)*");
    private static final Pattern TIME_SLOT_PATTERN = Pattern.compile(
        "^(MON|TUE|WED|THU|FRI|SAT|SUN)-([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)$"
    );
    private static final Set<String> EDUCATION_LEVELS = Set.of("graduated", "not graduated");

    public void validate(ApplicantProfile profile) {
        requireNotBlank(profile.profileId(), "profileId");
        requireNotBlank(profile.userId(), "userId");
        requireNotBlank(profile.studentId(), "studentId");
        requireNotBlank(profile.fullName(), "fullName");
        requireNotBlank(profile.programme(), "programme");
        requireNotBlank(profile.educationLevel(), "educationLevel");

        validateStudentId(profile.studentId());
        validateLettersOnlyValue(profile.fullName(), "fullName");
        validateLettersOnlyValue(profile.programme(), "programme");
        validateYearOfStudy(profile.yearOfStudy());
        validateEducationLevel(profile.educationLevel());
        validateOptionalSkillList(profile.skills(), "skills");
        validateRequiredAvailability(profile.availabilitySlots());
        validateRequiredLettersOnlyList(profile.desiredPositions(), "desiredPositions");
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private void validateStudentId(String studentId) {
        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            throw new IllegalArgumentException("studentId must be 8 to 12 digits.");
        }
    }

    private void validateLettersOnlyValue(String value, String fieldName) {
        if (!LETTERS_AND_SPACES_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must contain letters and spaces only.");
        }
    }

    private void validateYearOfStudy(int yearOfStudy) {
        if (yearOfStudy < 1 || yearOfStudy > 4) {
            throw new IllegalArgumentException("yearOfStudy must be between 1 and 4.");
        }
    }

    private void validateEducationLevel(String educationLevel) {
        String normalized = educationLevel.trim().toLowerCase();
        if (!EDUCATION_LEVELS.contains(normalized)) {
            throw new IllegalArgumentException("educationLevel must be Graduated or Not Graduated.");
        }
    }

    private void validateOptionalSkillList(java.util.List<String> values, String fieldName) {
        for (String value : values) {
            if (!SkillCatalog.isValidSkillValue(value)) {
                throw new IllegalArgumentException(
                    fieldName + " must use letters, numbers, spaces, +, #, . or - only."
                );
            }
        }
    }

    private void validateRequiredLettersOnlyList(java.util.List<String> values, String fieldName) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one " + fieldName + " value is required.");
        }

        for (String value : values) {
            validateLettersOnlyValue(value, fieldName);
        }
    }

    private void validateRequiredAvailability(java.util.List<String> availabilitySlots) {
        if (availabilitySlots.isEmpty()) {
            throw new IllegalArgumentException("At least one availability slot is required.");
        }

        for (String availabilitySlot : availabilitySlots) {
            Matcher matcher = TIME_SLOT_PATTERN.matcher(availabilitySlot);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                    "availabilitySlots must use the format DAY-HH:MM-HH:MM, for example MON-09:00-11:00."
                );
            }

            LocalTime startTime = LocalTime.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
            LocalTime endTime = LocalTime.of(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("availabilitySlots end time must be later than start time.");
            }
        }
    }
}
