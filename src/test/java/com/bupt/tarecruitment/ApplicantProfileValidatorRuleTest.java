package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantProfile;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;

import java.util.List;

/**
 * 验证申请人画像校验器的规则判断。
 */
public final class ApplicantProfileValidatorRuleTest {
    private ApplicantProfileValidatorRuleTest() {
    }

    // 这组规则测试不关心 repository 或 service，
    // 只关心 Validator 本身有没有把格式和字段约束守住。
    public static void main(String[] args) {
        ApplicantProfileValidator validator = new ApplicantProfileValidator();

        validator.validate(validProfile());

        expectValidationFailure(
            validator,
            new ApplicantProfile(
                "profile901",
                "ta901",
                "abc",
                "Rule Test Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Java"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            ),
            "studentId must be 8 to 12 digits."
        );

        expectValidationFailure(
            validator,
            new ApplicantProfile(
                "profile901",
                "ta901",
                "231229901",
                "Rule Test Applicant",
                "Computer Science",
                2,
                "Postgraduate",
                List.of("Java"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            ),
            "educationLevel must be Graduated or Not Graduated."
        );

        expectValidationFailure(
            validator,
            new ApplicantProfile(
                "profile901",
                "ta901",
                "231229901",
                "Rule Test Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Java"),
                List.of("Tuesday"),
                List.of("Teaching Assistant")
            ),
            "availabilitySlots must use the format DAY-HH:MM-HH:MM"
        );

        expectValidationFailure(
            validator,
            new ApplicantProfile(
                "profile901",
                "ta901",
                "231229901",
                "Rule Test Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Java3"),
                List.of("MON-09:00-11:00"),
                List.of("Teaching Assistant")
            ),
            "skills must contain letters and spaces only."
        );

        expectValidationFailure(
            validator,
            new ApplicantProfile(
                "profile901",
                "ta901",
                "231229901",
                "Rule Test Applicant",
                "Computer Science",
                2,
                "Not Graduated",
                List.of("Java"),
                List.of("MON-11:00-09:00"),
                List.of("Teaching Assistant")
            ),
            "availabilitySlots end time must be later than start time."
        );

        System.out.println("ApplicantProfileValidator rule test passed.");
    }

    private static ApplicantProfile validProfile() {
        return new ApplicantProfile(
            "profile901",
            "ta901",
            "231229901",
            "Rule Test Applicant",
            "Computer Science",
            2,
            "Not Graduated",
            List.of("Java", "Communication"),
            List.of("MON-09:00-11:00", "WED-14:00-16:00"),
            List.of("Teaching Assistant", "Lab Support")
        );
    }

    private static void expectValidationFailure(
        ApplicantProfileValidator validator,
        ApplicantProfile profile,
        String expectedMessagePart
    ) {
        try {
            validator.validate(profile);
            throw new IllegalStateException("Expected validator to reject profile for userId: " + profile.userId());
        } catch (IllegalArgumentException exception) {
            if (!exception.getMessage().contains(expectedMessagePart)) {
                throw new IllegalStateException("Unexpected validator message: " + exception.getMessage(), exception);
            }
        }
    }
}
