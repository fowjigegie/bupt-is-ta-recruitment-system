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
import java.util.Objects;

public final class ApplicantCvReviewSwingDemo {
    private final ApplicantCvReviewService reviewService;

    private final JTextField applicationIdField = new JTextField(24);
    private final JTextArea summaryArea = new JTextArea(10, 48);
    private final JTextArea cvContentArea = new JTextArea(16, 48);
    private final JTextArea resultArea = new JTextArea(5, 48);

    public ApplicantCvReviewSwingDemo(ApplicantCvReviewService reviewService) {
        this.reviewService = Objects.requireNonNull(reviewService);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Applicant CV Review Test UI");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel topPanel = new JPanel(new GridBagLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
            addField(topPanel, 0, "Application ID", applicationIdField);

            JPanel buttonPanel = new JPanel();
            JButton loadButton = new JButton("Load Applicant CV");
            JButton clearButton = new JButton("Clear");
            buttonPanel.add(loadButton);
            buttonPanel.add(clearButton);

            summaryArea.setEditable(false);
            summaryArea.setLineWrap(true);
            summaryArea.setWrapStyleWord(true);
            summaryArea.setBorder(BorderFactory.createTitledBorder("Applicant profile summary"));

            cvContentArea.setEditable(false);
            cvContentArea.setLineWrap(true);
            cvContentArea.setWrapStyleWord(true);
            cvContentArea.setBorder(BorderFactory.createTitledBorder("Stored CV content"));

            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setText("""
                Read-only CV review helper

                Notes:
                - Use an applicationId that already has a submitted CV, for example application001.
                - This demo shows application data, applicant profile, and application-specific CV text together.
                - Later organiser-side stories can reuse the same review logic.
                """);

            loadButton.addActionListener(event -> loadReview());
            clearButton.addActionListener(event -> clearForm());

            JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
            centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            centerPanel.add(new JScrollPane(summaryArea), BorderLayout.NORTH);
            centerPanel.add(new JScrollPane(cvContentArea), BorderLayout.CENTER);

            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(buttonPanel, BorderLayout.CENTER);
            frame.add(centerPanel, BorderLayout.SOUTH);
            frame.add(new JScrollPane(resultArea), BorderLayout.EAST);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void loadReview() {
        try {
            ApplicantCvReview review = reviewService.loadReviewByApplicationId(applicationIdField.getText().trim());
            ApplicantProfile profile = review.profile();
            com.bupt.tarecruitment.application.JobApplication application = review.application();

            summaryArea.setText("""
                applicationId: %s
                jobId: %s
                applicantUserId: %s
                applicationCvFileName: %s
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
                application.applicationId(),
                application.jobId(),
                application.applicantUserId(),
                application.cvFileName(),
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
            cvContentArea.setText(review.cvContent());
            resultArea.setText("Application-specific CV loaded successfully for review.");
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to load applicant CV.\n\n" + exception.getMessage());
        }
    }

    private void clearForm() {
        applicationIdField.setText("");
        summaryArea.setText("");
        cvContentArea.setText("");
        resultArea.setText("Form cleared.");
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
