package com.bupt.tarecruitment.applicant;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ApplicantProfileSwingDemo {
    private final ApplicantProfileService service;
    private final ApplicantProfileIdGenerator profileIdGenerator;
    private final String fixedUserId;
    private final boolean lockUserId;

    private final JTextField userIdField = new JTextField(24);
    private final JTextField studentIdField = new JTextField(24);
    private final JTextField fullNameField = new JTextField(24);
    private final JTextField programmeField = new JTextField(24);
    private final JTextField yearOfStudyField = new JTextField(24);
    private final JTextField educationLevelField = new JTextField(24);
    private final JTextField skillsField = new JTextField(24);
    private final JTextField availabilityField = new JTextField(24);
    private final JTextField desiredPositionsField = new JTextField(24);
    private final JTextArea resultArea = new JTextArea(14, 48);

    public ApplicantProfileSwingDemo(ApplicantProfileService service, ApplicantProfileIdGenerator profileIdGenerator) {
        this(service, profileIdGenerator, "", false);
    }

    public ApplicantProfileSwingDemo(
        ApplicantProfileService service,
        ApplicantProfileIdGenerator profileIdGenerator,
        String fixedUserId,
        boolean lockUserId
    ) {
        this.service = Objects.requireNonNull(service);
        this.profileIdGenerator = Objects.requireNonNull(profileIdGenerator);
        this.fixedUserId = fixedUserId == null ? "" : fixedUserId.trim();
        this.lockUserId = lockUserId && !this.fixedUserId.isBlank();
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("US-01 Applicant Profile Test UI");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            addField(formPanel, 0, "Applicant userId", userIdField);
            addField(formPanel, 1, "Student ID (8-12 digits)", studentIdField);
            addField(formPanel, 2, "Full name (letters/spaces)", fullNameField);
            addField(formPanel, 3, "Programme (letters/spaces)", programmeField);
            addField(formPanel, 4, "Year of study (1-4)", yearOfStudyField);
            addField(formPanel, 5, "Education level", educationLevelField);
            addField(formPanel, 6, "Skills (; or ,)", skillsField);
            addField(formPanel, 7, "Availability (DAY-HH:MM-HH:MM)", availabilityField);
            addField(formPanel, 8, "Desired positions (; or ,)", desiredPositionsField);

            JPanel buttonPanel = new JPanel();
            JButton createButton = new JButton("Create Profile");
            JButton loadButton = new JButton("Load Profile");
            JButton clearButton = new JButton("Clear");
            buttonPanel.add(createButton);
            buttonPanel.add(loadButton);
            buttonPanel.add(clearButton);

            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setText("""
                US-01 Applicant Profile Test UI

                Notes:
                - Use a new applicant userId such as ta002, ta003, or ta101.
                - Student ID must be 8-12 digits and cannot be reused by another applicant.
                - Full name, programme, skills, and desired positions use letters and spaces only.
                - Year of study must be 1 to 4.
                - Education level must be Graduated or Not Graduated.
                - Availability must use DAY-HH:MM-HH:MM, for example MON-09:00-11:00.
                - Click Load Profile to pull an existing profile by userId.
                """);

            if (!fixedUserId.isBlank()) {
                userIdField.setText(fixedUserId);
                userIdField.setEditable(!lockUserId);
                tryLoadFixedProfile();
            }

            createButton.addActionListener(event -> createProfile());
            loadButton.addActionListener(event -> loadProfile());
            clearButton.addActionListener(event -> clearForm());

            frame.add(formPanel, BorderLayout.NORTH);
            frame.add(buttonPanel, BorderLayout.CENTER);
            frame.add(new JScrollPane(resultArea), BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void createProfile() {
        try {
            ApplicantProfile profile = new ApplicantProfile(
                profileIdGenerator.nextProfileId(),
                userIdField.getText().trim(),
                studentIdField.getText().trim(),
                fullNameField.getText().trim(),
                programmeField.getText().trim(),
                Integer.parseInt(yearOfStudyField.getText().trim()),
                educationLevelField.getText().trim(),
                splitList(skillsField.getText()),
                splitList(availabilityField.getText()),
                splitList(desiredPositionsField.getText())
            );

            ApplicantProfile savedProfile = service.createProfile(profile);
            renderProfile("Profile created successfully.", savedProfile);
        } catch (NumberFormatException exception) {
            resultArea.setText("Failed to create profile.\n\nYear of study must be an integer between 1 and 4.");
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to create profile.\n\n" + exception.getMessage());
        }
    }

    private void loadProfile() {
        String userId = userIdField.getText().trim();
        if (userId.isBlank()) {
            resultArea.setText("Please enter an applicant userId before loading a profile.");
            return;
        }

        Optional<ApplicantProfile> profile = service.getProfileByUserId(userId);
        if (profile.isEmpty()) {
            resultArea.setText("No profile found for userId " + userId + ".");
            return;
        }

        fillForm(profile.get());
        renderProfile("Profile loaded successfully.", profile.get());
    }

    private void clearForm() {
        userIdField.setText(lockUserId ? fixedUserId : "");
        studentIdField.setText("");
        fullNameField.setText("");
        programmeField.setText("");
        yearOfStudyField.setText("");
        educationLevelField.setText("");
        skillsField.setText("");
        availabilityField.setText("");
        desiredPositionsField.setText("");
        resultArea.setText("Form cleared.");
    }

    private void tryLoadFixedProfile() {
        Optional<ApplicantProfile> profile = service.getProfileByUserId(fixedUserId);
        if (profile.isPresent()) {
            fillForm(profile.get());
            renderProfile("Existing profile loaded for the signed-in applicant.", profile.get());
        }
    }

    private void fillForm(ApplicantProfile profile) {
        userIdField.setText(profile.userId());
        studentIdField.setText(profile.studentId());
        fullNameField.setText(profile.fullName());
        programmeField.setText(profile.programme());
        yearOfStudyField.setText(Integer.toString(profile.yearOfStudy()));
        educationLevelField.setText(profile.educationLevel());
        skillsField.setText(String.join("; ", profile.skills()));
        availabilityField.setText(String.join("; ", profile.availabilitySlots()));
        desiredPositionsField.setText(String.join("; ", profile.desiredPositions()));
    }

    private void renderProfile(String title, ApplicantProfile profile) {
        resultArea.setText("""
            %s

            profileId: %s
            userId: %s
            studentId: %s
            fullName: %s
            programme: %s
            yearOfStudy: %d
            educationLevel: %s
            skills: %s
            availabilitySlots: %s
            desiredPositions: %s
            """.formatted(
            title,
            profile.profileId(),
            profile.userId(),
            profile.studentId(),
            profile.fullName(),
            profile.programme(),
            profile.yearOfStudy(),
            profile.educationLevel(),
            String.join(", ", profile.skills()),
            String.join(", ", profile.availabilitySlots()),
            String.join(", ", profile.desiredPositions())
        ));
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

    private void addField(JPanel panel, int row, String labelText, JTextField field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 4, 4, 12);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(4, 4, 4, 4);

        panel.add(new JLabel(labelText), labelConstraints);
        panel.add(field, fieldConstraints);
    }
}
