package com.bupt.tarecruitment.applicant;

import java.util.List;
import java.util.Objects;

public record ApplicantProfile(
    String profileId,
    String userId,
    String studentId,
    String fullName,
    String programme,
    int yearOfStudy,
    String educationLevel,
    List<String> skills,
    List<String> availabilitySlots,
    List<String> desiredPositions
) {
    public ApplicantProfile {
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(fullName);
        Objects.requireNonNull(programme);
        Objects.requireNonNull(educationLevel);
        Objects.requireNonNull(skills);
        Objects.requireNonNull(availabilitySlots);
        Objects.requireNonNull(desiredPositions);

        skills = List.copyOf(skills);
        availabilitySlots = List.copyOf(availabilitySlots);
        desiredPositions = List.copyOf(desiredPositions);
    }
}
