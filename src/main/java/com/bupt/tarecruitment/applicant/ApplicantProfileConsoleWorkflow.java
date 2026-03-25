package com.bupt.tarecruitment.applicant;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public final class ApplicantProfileConsoleWorkflow {
    private final ApplicantProfileService service;
    private final ApplicantProfileIdGenerator profileIdGenerator;
    private final Scanner scanner;
    private final PrintStream output;

    public ApplicantProfileConsoleWorkflow(
        ApplicantProfileService service,
        ApplicantProfileIdGenerator profileIdGenerator,
        InputStream input,
        PrintStream output
    ) {
        this.service = Objects.requireNonNull(service);
        this.profileIdGenerator = Objects.requireNonNull(profileIdGenerator);
        this.scanner = new Scanner(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        output.println("US-01 Create Applicant Profile Demo");
        output.println("This flow uses a mocked applicant context and text-file storage.");
        output.println("Use a new applicant userId such as ta002 if ta001 already has a profile.");
        output.println("Rules: studentId must be 8-12 digits, yearOfStudy must be 1-4, and educationLevel must be Graduated or Not Graduated.");
        output.println("Availability must use DAY-HH:MM-HH:MM, for example MON-09:00-11:00.");

        boolean running = true;
        while (running) {
            output.println();
            output.println("Choose an option:");
            output.println("1. Create applicant profile");
            output.println("2. View applicant profile");
            output.println("3. Exit");
            output.print("> ");

            String command = scanner.nextLine().trim();
            switch (command) {
                case "1" -> createProfile();
                case "2" -> viewProfile();
                case "3" -> running = false;
                default -> output.println("Unknown option. Please choose 1, 2, or 3.");
            }
        }
    }

    private void createProfile() {
        output.println();
        output.println("Create Applicant Profile");

        String userId = promptNonBlank("Applicant userId");
        if (service.getProfileByUserId(userId).isPresent()) {
            output.println("A profile already exists for userId " + userId + ".");
            output.println("Use the view option now, or wait for US-05 to edit this profile.");
            return;
        }

        String profileId = profileIdGenerator.nextProfileId();
        String studentId = promptNonBlank("Student ID");
        String fullName = promptNonBlank("Full name");
        String programme = promptNonBlank("Programme");
        int yearOfStudy = promptIntInRange("Year of study", 1, 4);
        String educationLevel = promptNonBlank("Education level (Graduated or Not Graduated)");
        List<String> skills = promptList("Skills separated by ';' or ',' (optional)");
        List<String> availabilitySlots = promptRequiredList("Availability slots separated by ';' or ','");
        List<String> desiredPositions = promptRequiredList("Desired positions separated by ';' or ','");

        ApplicantProfile profile = new ApplicantProfile(
            profileId,
            userId,
            studentId,
            fullName,
            programme,
            yearOfStudy,
            educationLevel,
            skills,
            availabilitySlots,
            desiredPositions
        );

        try {
            service.createProfile(profile);
            output.println("Profile created successfully.");
            printProfile(profile);
        } catch (IllegalArgumentException exception) {
            output.println("Failed to create profile: " + exception.getMessage());
        }
    }

    private void viewProfile() {
        output.println();
        output.println("View Applicant Profile");
        String userId = promptNonBlank("Applicant userId");
        Optional<ApplicantProfile> profile = service.getProfileByUserId(userId);

        if (profile.isEmpty()) {
            output.println("No profile found for userId " + userId + ".");
            return;
        }

        printProfile(profile.get());
    }

    private void printProfile(ApplicantProfile profile) {
        output.println("Profile summary:");
        output.println(" - profileId: " + profile.profileId());
        output.println(" - userId: " + profile.userId());
        output.println(" - studentId: " + profile.studentId());
        output.println(" - fullName: " + profile.fullName());
        output.println(" - programme: " + profile.programme());
        output.println(" - yearOfStudy: " + profile.yearOfStudy());
        output.println(" - educationLevel: " + profile.educationLevel());
        output.println(" - skills: " + String.join(", ", profile.skills()));
        output.println(" - availabilitySlots: " + String.join(", ", profile.availabilitySlots()));
        output.println(" - desiredPositions: " + String.join(", ", profile.desiredPositions()));
    }

    private String promptNonBlank(String label) {
        while (true) {
            output.print(label + ": ");
            String value = scanner.nextLine().trim();
            if (!value.isBlank()) {
                return value;
            }
            output.println(label + " cannot be blank.");
        }
    }

    private String promptOptional(String label) {
        output.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private int promptIntInRange(String label, int minimum, int maximum) {
        while (true) {
            output.print(label + ": ");
            String value = scanner.nextLine().trim();
            try {
                int parsed = Integer.parseInt(value);
                if (parsed >= minimum && parsed <= maximum) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }

            output.println(label + " must be between " + minimum + " and " + maximum + ".");
        }
    }

    private List<String> promptList(String label) {
        String rawValue = promptOptional(label);
        return splitList(rawValue);
    }

    private List<String> promptRequiredList(String label) {
        while (true) {
            String rawValue = promptOptional(label);
            List<String> values = splitList(rawValue);
            if (!values.isEmpty()) {
                return values;
            }

            output.println("At least one value is required.");
        }
    }

    private List<String> splitList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String normalized = rawValue.replace(',', ';');
        return List.of(normalized.split(";")).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }
}
