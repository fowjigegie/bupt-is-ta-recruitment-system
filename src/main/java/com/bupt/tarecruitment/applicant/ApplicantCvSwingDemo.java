package com.bupt.tarecruitment.applicant;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class ApplicantCvSwingDemo {
    private final ApplicantCvService cvService;

    private final JTextField userIdField = new JTextField(24);
    private final JTextField cvReferenceField = new JTextField(32);
    private final JTextArea cvContentArea = new JTextArea(16, 48);
    private final JTextArea resultArea = new JTextArea(10, 48);

    public ApplicantCvSwingDemo(ApplicantCvService cvService) {
        this.cvService = Objects.requireNonNull(cvService);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("US-02 Submit CV Test UI");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            cvReferenceField.setEditable(false);
            addField(formPanel, 0, "Applicant userId", userIdField);
            addField(formPanel, 1, "Stored CV path", cvReferenceField);

            cvContentArea.setLineWrap(true);
            cvContentArea.setWrapStyleWord(true);
            cvContentArea.setBorder(BorderFactory.createTitledBorder("CV text content"));

            JPanel buttonPanel = new JPanel();
            JButton importButton = new JButton("Import .txt");
            JButton submitButton = new JButton("Submit CV");
            JButton loadButton = new JButton("Load CV");
            JButton clearButton = new JButton("Clear");
            buttonPanel.add(importButton);
            buttonPanel.add(submitButton);
            buttonPanel.add(loadButton);
            buttonPanel.add(clearButton);

            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setText("""
                US-02 Submit CV Test UI

                Notes:
                - Use an applicant userId that already has a profile, for example ta001.
                - Paste CV content directly, or import a local .txt file.
                - Submit CV stores text in data/cvs/<userId>/current.txt.
                - Repeated submission overwrites the applicant's current CV file.
                - Load CV reads the current stored CV back into this editor.
                """);

            importButton.addActionListener(event -> importTxtFile(frame));
            submitButton.addActionListener(event -> submitCv());
            loadButton.addActionListener(event -> loadCv());
            clearButton.addActionListener(event -> clearForm());

            frame.add(formPanel, BorderLayout.NORTH);
            frame.add(new JScrollPane(cvContentArea), BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.WEST);
            frame.add(new JScrollPane(resultArea), BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void importTxtFile(JFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a .txt CV file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));

        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();
        if (!selectedFile.getFileName().toString().toLowerCase().endsWith(".txt")) {
            resultArea.setText("Only .txt CV files are supported.");
            return;
        }

        try {
            String content = Files.readString(selectedFile, StandardCharsets.UTF_8);
            cvContentArea.setText(content);
            resultArea.setText("Local .txt file loaded into the editor.\n\nSubmit CV to save it into system storage.");
        } catch (IOException exception) {
            resultArea.setText("Failed to read the selected .txt file.\n\n" + exception.getMessage());
        }
    }

    private void submitCv() {
        try {
            ApplicantProfile updatedProfile = cvService.submitCv(
                userIdField.getText().trim(),
                cvContentArea.getText()
            );

            cvReferenceField.setText(updatedProfile.cvFileName());
            resultArea.setText("""
                CV submitted successfully.

                userId: %s
                stored path: %s
                content length: %d characters
                """.formatted(
                updatedProfile.userId(),
                updatedProfile.cvFileName(),
                cvContentArea.getText().length()
            ));
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to submit CV.\n\n" + exception.getMessage());
        }
    }

    private void loadCv() {
        String userId = userIdField.getText().trim();
        if (userId.isBlank()) {
            resultArea.setText("Please enter an applicant userId before loading CV content.");
            return;
        }

        try {
            String cvContent = cvService.loadCvContentByUserId(userId);
            Optional<String> cvReference = cvService.getCvReferenceByUserId(userId);
            cvReferenceField.setText(cvReference.orElse(""));
            cvContentArea.setText(cvContent);
            resultArea.setText("""
                CV loaded successfully.

                userId: %s
                stored path: %s
                """.formatted(
                userId,
                cvReference.orElse("(blank)")
            ));
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to load CV.\n\n" + exception.getMessage());
        }
    }

    private void clearForm() {
        userIdField.setText("");
        cvReferenceField.setText("");
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
