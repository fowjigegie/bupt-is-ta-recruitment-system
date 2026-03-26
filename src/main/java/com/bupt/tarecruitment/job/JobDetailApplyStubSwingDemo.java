package com.bupt.tarecruitment.job;

import com.bupt.tarecruitment.applicant.ApplicantCv;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.application.JobApplicationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

public final class JobDetailApplyStubSwingDemo {
    private final String jobId;
    private final String applicantUserId;
    private final ApplicantCvLibraryService cvLibraryService;
    private final JobApplicationService applicationService;

    public JobDetailApplyStubSwingDemo(
        String jobId,
        String applicantUserId,
        ApplicantCvLibraryService cvLibraryService,
        JobApplicationService applicationService
    ) {
        this.jobId = Objects.requireNonNull(jobId).trim();
        this.applicantUserId = Objects.requireNonNull(applicantUserId).trim();
        this.cvLibraryService = Objects.requireNonNull(cvLibraryService);
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Job Detail Page (stub)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JLabel titleLabel = new JLabel("This is the job detail page (stub).");
            titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            frame.add(titleLabel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton applyButton = new JButton("APPLY");
            buttonPanel.add(applyButton);
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

            applyButton.addActionListener(event -> applyWithCvSelection(frame));

            frame.add(bottomPanel, BorderLayout.SOUTH);
            frame.setSize(520, 240);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void applyWithCvSelection(JFrame parent) {
        try {
            if (jobId.isBlank() || applicantUserId.isBlank()) {
                JOptionPane.showMessageDialog(parent, "Missing jobId or applicantUserId.", "Apply failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<ApplicantCv> cvs = cvLibraryService.listCvsByUserId(applicantUserId);
            if (cvs.isEmpty()) {
                JOptionPane.showMessageDialog(
                    parent,
                    "You must create at least one CV before applying.\n\nUse us02-ui to create CVs in your library.",
                    "No CV available",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            JComboBox<ApplicantCv> comboBox = new JComboBox<>(cvs.toArray(new ApplicantCv[0]));
            comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel label = new JLabel();
                if (value == null) {
                    label.setText("");
                } else {
                    label.setText("%s | %s".formatted(value.cvId(), value.title()));
                }
                if (isSelected) {
                    label.setOpaque(true);
                }
                return label;
            });

            int choice = JOptionPane.showConfirmDialog(
                parent,
                comboBox,
                "Select a CV for this application",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            ApplicantCv selected = (ApplicantCv) comboBox.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, "Please select a CV.", "Apply cancelled", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JobApplication application = applicationService.applyToJobWithCv(applicantUserId, jobId, selected.cvId());
            JOptionPane.showMessageDialog(
                parent,
                """
                Application submitted successfully.

                applicationId: %s
                jobId: %s
                applicantUserId: %s
                selected cvId: %s
                """.formatted(
                    application.applicationId(),
                    application.jobId(),
                    application.applicantUserId(),
                    application.cvId()
                ),
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(parent, exception.getMessage(), "Apply failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

