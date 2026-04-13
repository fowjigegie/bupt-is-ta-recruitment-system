package com.bupt.tarecruitment.applicant;

import java.time.LocalTime;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校验申请人画像字段是否满足业务规则。
 */
public final class ApplicantProfileValidator {
    // 学号规则：8~12 位数字
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("\\d{8,12}");
    // 只允许字母和空格（例如 "Computer Science"）
    private static final Pattern LETTERS_AND_SPACES_PATTERN = Pattern.compile("[A-Za-z]+(?: [A-Za-z]+)*");
    // 可用时间格式：DAY-HH:MM-HH:MM
    private static final Pattern TIME_SLOT_PATTERN = Pattern.compile(
        "^(MON|TUE|WED|THU|FRI|SAT|SUN)-([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)$"
    );
    // 只允许两个值：Graduated / Not Graduated（忽略大小写）
    private static final Set<String> EDUCATION_LEVELS = Set.of("graduated", "not graduated");

    // 这个类只负责"字段内容是否合法"。
    // 它不负责"这个 user 是否存在""studentId 是否重复"这种业务规则，
    // 那些规则在 ApplicantProfileService 里做。
    public void validate(ApplicantProfile profile) {
        // 第一步：必填字段检查。
        requireNotBlank(profile.profileId(), "profileId");
        requireNotBlank(profile.userId(), "userId");
        requireNotBlank(profile.studentId(), "studentId");
        requireNotBlank(profile.fullName(), "fullName");
        requireNotBlank(profile.programme(), "programme");
        requireNotBlank(profile.educationLevel(), "educationLevel");

        // 第二步：格式与范围校验。
        validateStudentId(profile.studentId());
        validateLettersOnlyValue(profile.fullName(), "fullName");
        validateLettersOnlyValue(profile.programme(), "programme");
        validateYearOfStudy(profile.yearOfStudy());
        validateEducationLevel(profile.educationLevel());
        validateOptionalLettersOnlyList(profile.skills(), "skills");
        validateRequiredAvailability(profile.availabilitySlots());
        validateRequiredLettersOnlyList(profile.desiredPositions(), "desiredPositions");
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }

    private void validateStudentId(String studentId) {
        // studentId 必须满足 8~12 位数字。
        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            throw new IllegalArgumentException("studentId must be 8 to 12 digits.");
        }
    }

    private void validateLettersOnlyValue(String value, String fieldName) {
        // 只允许字母和空格，避免出现数字/符号导致 UI 展示异常。
        if (!LETTERS_AND_SPACES_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must contain letters and spaces only.");
        }
    }

    private void validateYearOfStudy(int yearOfStudy) {
        // 当前只支持 1~4 年级。
        if (yearOfStudy < 1 || yearOfStudy > 4) {
            throw new IllegalArgumentException("yearOfStudy must be between 1 and 4.");
        }
    }

    private void validateEducationLevel(String educationLevel) {
        // 统一转换为小写再判断。
        String normalized = educationLevel.trim().toLowerCase();
        if (!EDUCATION_LEVELS.contains(normalized)) {
            throw new IllegalArgumentException("educationLevel must be Graduated or Not Graduated.");
        }
    }

    private void validateOptionalLettersOnlyList(java.util.List<String> values, String fieldName) {
        // skills 允许是可选列表，但只要填了，每一项都必须符合"仅字母和空格"的规则。
        for (String value : values) {
            validateLettersOnlyValue(value, fieldName);
        }
    }

    private void validateRequiredLettersOnlyList(java.util.List<String> values, String fieldName) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one " + fieldName + " value is required.");
        }

        // 逐项检查是否只包含字母和空格。
        validateOptionalLettersOnlyList(values, fieldName);
    }

    private void validateRequiredAvailability(java.util.List<String> availabilitySlots) {
        if (availabilitySlots.isEmpty()) {
            throw new IllegalArgumentException("At least one availability slot is required.");
        }

        // availability 的格式后面还会被排班/冲突判断复用，
        // 所以这里要提前保证格式统一。
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
