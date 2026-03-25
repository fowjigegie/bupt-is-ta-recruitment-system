package com.bupt.tarecruitment.applicant;

import com.bupt.tarecruitment.application.JobApplication;

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
import java.util.stream.Collectors;

public final class ApplicantCvSwingDemo {
    private final ApplicantCvLibraryService cvLibraryService;
    private final ApplicantCvService cvService;

    private final JTextField ownerUserIdField = new JTextField(18);
    private final JTextField cvTitleField = new JTextField(24);
    private final JTextField cvIdField = new JTextField(18);
    private final JTextField applicationIdField = new JTextField(18);
    private final JTextField cvReferenceField = new JTextField(30);
    private final JTextArea cvContentArea = new JTextArea(16, 48);
    private final JTextArea cvListArea = new JTextArea(10, 48);
    private final JTextArea resultArea = new JTextArea(8, 48);

    public ApplicantCvSwingDemo(ApplicantCvLibraryService cvLibraryService, ApplicantCvService cvService) {
        this.cvLibraryService = Objects.requireNonNull(cvLibraryService);
        this.cvService = Objects.requireNonNull(cvService);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("US-02 CV Library and Attachment Test UI");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            cvIdField.setEditable(false);
            cvReferenceField.setEditable(false);
            addField(formPanel, 0, "Applicant userId", ownerUserIdField);
            addField(formPanel, 1, "CV title", cvTitleField);
            addField(formPanel, 2, "Selected CV ID", cvIdField);
            addField(formPanel, 3, "Application ID", applicationIdField);
            addField(formPanel, 4, "Stored CV path", cvReferenceField);

            cvContentArea.setLineWrap(true);
            cvContentArea.setWrapStyleWord(true);
            cvContentArea.setBorder(BorderFactory.createTitledBorder("CV text content"));

            cvListArea.setEditable(false);
            cvListArea.setLineWrap(true);
            cvListArea.setWrapStyleWord(true);
            cvListArea.setBorder(BorderFactory.createTitledBorder("CVs owned by this applicant"));

            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setText("""
                US-02 CV Library and Attachment Test UI

                Flow:
                1. Enter applicant userId and CV title, then create a CV from pasted/imported text.
                2. The system gives that CV its own cvId, for example cv003.
                3. Each applicant can keep up to 10 CVs in the current design.
                4. Enter an applicationId and attach the selected cvId to that application.
                5. Review screens later read the application's selected cvId.
                """);

            JPanel buttonPanel = new JPanel();
            JButton importButton = new JButton("Import .txt");
            JButton createButton = new JButton("Create CV");
            JButton loadCvButton = new JButton("Load CV");
            JButton listButton = new JButton("List User CVs");
            JButton attachButton = new JButton("Attach to Application");
            JButton clearButton = new JButton("Clear");
            buttonPanel.add(importButton);
            buttonPanel.add(createButton);
            buttonPanel.add(loadCvButton);
            buttonPanel.add(listButton);
            buttonPanel.add(attachButton);
            buttonPanel.add(clearButton);

            importButton.addActionListener(event -> importTxtFile(frame));
            createButton.addActionListener(event -> createCv());
            loadCvButton.addActionListener(event -> loadCv());
            listButton.addActionListener(event -> listCvs());
            attachButton.addActionListener(event -> attachCvToApplication());
            clearButton.addActionListener(event -> clearForm());

            JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
            centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            centerPanel.add(new JScrollPane(cvContentArea), BorderLayout.CENTER);
            centerPanel.add(new JScrollPane(cvListArea), BorderLayout.SOUTH);

            frame.add(formPanel, BorderLayout.NORTH);
            frame.add(centerPanel, BorderLayout.CENTER);
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
            cvContentArea.setText(Files.readString(selectedFile, StandardCharsets.UTF_8));
            resultArea.setText("Local .txt file loaded into the editor.\n\nCreate CV to add it into the applicant's CV library.");
        } catch (IOException exception) {
            resultArea.setText("Failed to read the selected .txt file.\n\n" + exception.getMessage());
        }
    }

    private void createCv() {
        try {
            ApplicantCv applicantCv = cvLibraryService.createCv(
                ownerUserIdField.getText().trim(),
                cvTitleField.getText().trim(),
                cvContentArea.getText()
            );
            cvIdField.setText(applicantCv.cvId());
            cvReferenceField.setText(applicantCv.fileName());
            resultArea.setText("""
                CV created successfully.

                cvId: %s
                ownerUserId: %s
                title: %s
                fileName: %s
                """.formatted(
                applicantCv.cvId(),
                applicantCv.ownerUserId(),
                applicantCv.title(),
                applicantCv.fileName()
            ));
            listCvs();
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to create CV.\n\n" + exception.getMessage());
        }
    }

    private void loadCv() {
        String cvId = cvIdField.getText().trim();
        if (cvId.isBlank()) {
            resultArea.setText("Please create or select a cvId before loading CV content.");
            return;
        }

        try {
            ApplicantCv applicantCv = cvLibraryService.getCvById(cvId);
            ownerUserIdField.setText(applicantCv.ownerUserId());
            cvTitleField.setText(applicantCv.title());
            cvReferenceField.setText(applicantCv.fileName());
            cvContentArea.setText(cvLibraryService.loadCvContentByCvId(cvId));
            resultArea.setText("CV loaded successfully from the applicant's CV library.");
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to load CV.\n\n" + exception.getMessage());
        }
    }

    private void listCvs() {
        String ownerUserId = ownerUserIdField.getText().trim();
        if (ownerUserId.isBlank()) {
            resultArea.setText("Please enter an applicant userId before listing CVs.");
            return;
        }

        try {
            String cvList = cvLibraryService.listCvsByUserId(ownerUserId).stream()
                .map(applicantCv -> "%s | %s | %s".formatted(applicantCv.cvId(), applicantCv.title(), applicantCv.fileName()))
                .collect(Collectors.joining(System.lineSeparator()));

            cvListArea.setText(cvList.isBlank() ? "(no CVs found for this applicant)" : cvList);
            resultArea.setText("CV library loaded for applicant " + ownerUserId + ".");
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to list CVs.\n\n" + exception.getMessage());
        }
    }

    private void attachCvToApplication() {
        try {
            JobApplication updatedApplication = cvService.attachCvToApplication(
                applicationIdField.getText().trim(),
                cvIdField.getText().trim()
            );
            resultArea.setText("""
                CV attached to application successfully.

                applicationId: %s
                applicantUserId: %s
                selected cvId: %s
                """.formatted(
                updatedApplication.applicationId(),
                updatedApplication.applicantUserId(),
                updatedApplication.cvId()
            ));
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to attach CV to application.\n\n" + exception.getMessage());
        }
    }

    private void clearForm() {
        ownerUserIdField.setText("");
        cvTitleField.setText("");
        cvIdField.setText("");
        applicationIdField.setText("");
        cvReferenceField.setText("");
        cvContentArea.setText("");
        cvListArea.setText("");
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
