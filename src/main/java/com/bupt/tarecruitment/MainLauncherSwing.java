package com.bupt.tarecruitment;

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
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;

public final class MainLauncherSwing {
    private final ProjectModuleFactory moduleFactory;

    private final JTextField applicantUserIdField = new JTextField("ta001", 18);
    private final JTextField jobIdField = new JTextField("job003", 18);
    private final JTextArea statusArea = new JTextArea(10, 48);

    public MainLauncherSwing(ProjectModuleFactory moduleFactory) {
        this.moduleFactory = Objects.requireNonNull(moduleFactory);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("BUPT TA Recruitment - Main UI");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            root.add(buildHeader(), BorderLayout.NORTH);
            root.add(buildLauncherPanel(), BorderLayout.CENTER);
            root.add(new JScrollPane(statusArea), BorderLayout.SOUTH);

            statusArea.setEditable(false);
            statusArea.setLineWrap(true);
            statusArea.setWrapStyleWord(true);
            statusArea.setText("""
                Main launcher is ready.

                Recommended flow:
                1. Open Auth UI if you want a login/register window.
                2. Open Applicant Profile UI and create a profile.
                3. Open CV Library UI and create a CV.
                4. Open Job Posting UI to create an OPEN job.
                5. Open Apply to Job UI with an applicant userId and jobId.
                6. Open CV Review UI to inspect an application-specific CV.

                US03/find_job is intentionally not used here.
                """);

            frame.add(root, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        panel.setBackground(new Color(245, 248, 252));

        JLabel title = new JLabel("BUPT International School TA Recruitment System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Unified Swing entry for US00, US01, US02, US04, US11, and CV review.");
        subtitle.setForeground(new Color(70, 85, 105));
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildLauncherPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 12));
        panel.add(buildQuickLaunchPanel());
        panel.add(buildFlowPanel());
        return panel;
    }

    private JPanel buildQuickLaunchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);

        JLabel title = new JLabel("Module UIs");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        c.gridy = 0;
        panel.add(title, c);

        c.gridy = 1;
        panel.add(createLaunchButton("Open Auth UI", () -> {
            moduleFactory.createAuthUi().show();
            setStatus("Opened Auth UI.");
        }), c);

        c.gridy = 2;
        panel.add(createLaunchButton("Open Applicant Profile UI", () -> {
            moduleFactory.createUs01Ui().show();
            setStatus("Opened US01 Applicant Profile UI.");
        }), c);

        c.gridy = 3;
        panel.add(createLaunchButton("Open CV Library UI", () -> {
            moduleFactory.createUs02Ui().show();
            setStatus("Opened US02 CV Library UI.");
        }), c);

        c.gridy = 4;
        panel.add(createLaunchButton("Open Job Posting UI", () -> {
            moduleFactory.createJobPostingUi().show();
            setStatus("Opened US11 Job Posting UI.");
        }), c);

        c.gridy = 5;
        panel.add(createLaunchButton("Open CV Review UI", () -> {
            moduleFactory.createCvReviewUi().show();
            setStatus("Opened CV Review UI.");
        }), c);

        return panel;
    }

    private JPanel buildFlowPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 12, 0);

        JLabel title = new JLabel("Apply Flow");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        panel.add(title, c);

        c.gridwidth = 1;
        c.gridy = 1;
        c.insets = new Insets(4, 0, 4, 12);
        panel.add(new JLabel("Applicant userId"), c);

        c.gridx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        panel.add(applicantUserIdField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(4, 0, 4, 12);
        panel.add(new JLabel("Job ID"), c);

        c.gridx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        panel.add(jobIdField, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.insets = new Insets(12, 0, 6, 0);
        panel.add(createLaunchButton("Open Apply to Job UI", this::openApplyUi), c);

        c.gridy = 4;
        JTextArea helperArea = new JTextArea("""
            Suggested demo values:
            - applicant userId: ta001
            - jobId: job003

            If you create a new applicant profile or a new job posting,
            update these fields before opening the apply window.
            """);
        helperArea.setEditable(false);
        helperArea.setLineWrap(true);
        helperArea.setWrapStyleWord(true);
        helperArea.setOpaque(false);
        helperArea.setBorder(null);
        panel.add(helperArea, c);

        return panel;
    }

    private JButton createLaunchButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void openApplyUi() {
        String applicantUserId = applicantUserIdField.getText().trim();
        String jobId = jobIdField.getText().trim();
        if (applicantUserId.isBlank() || jobId.isBlank()) {
            setStatus("Applicant userId and jobId are required before opening US04.");
            return;
        }

        moduleFactory.createUs04Ui(jobId, applicantUserId).show();
        setStatus("Opened US04 Apply UI for applicantUserId=%s and jobId=%s.".formatted(applicantUserId, jobId));
    }

    private void setStatus(String message) {
        statusArea.setText(message);
    }
}
