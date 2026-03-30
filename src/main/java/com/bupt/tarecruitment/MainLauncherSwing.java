package com.bupt.tarecruitment;

import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;

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
import java.util.Optional;

public final class MainLauncherSwing {
    private final ProjectModuleFactory moduleFactory;

    private final JTextField applicantUserIdField = new JTextField(18);
    private final JTextField jobIdField = new JTextField("job003", 18);
    private final JTextArea statusArea = new JTextArea(12, 48);

    private JButton authButton;
    private JButton applicantProfileButton;
    private JButton cvLibraryButton;
    private JButton jobPostingButton;
    private JButton cvReviewButton;
    private JButton applyButton;

    private String lastActionMessage = """
        Main launcher is ready.

        Please sign in first.
        - APPLICANT can open US01, US02, and US04.
        - MO can open US11 and CV review.
        - ADMIN currently has no dedicated feature page in this launcher.
        """;

    public MainLauncherSwing(ProjectModuleFactory moduleFactory) {
        this.moduleFactory = Objects.requireNonNull(moduleFactory);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("BUPT TA Recruitment - Main UI");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            applicantUserIdField.setEditable(false);

            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            root.add(buildHeader(), BorderLayout.NORTH);
            root.add(buildLauncherPanel(), BorderLayout.CENTER);
            root.add(new JScrollPane(statusArea), BorderLayout.SOUTH);

            statusArea.setEditable(false);
            statusArea.setLineWrap(true);
            statusArea.setWrapStyleWord(true);

            frame.add(root, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);

            refreshSessionState();
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

        JLabel subtitle = new JLabel("Unified Swing entry with a shared US-00 login session.");
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

        authButton = createLaunchButton("Open Auth UI", this::openAuthUi);
        c.gridy = 1;
        panel.add(authButton, c);

        applicantProfileButton = createLaunchButton("Open Applicant Profile UI", this::openApplicantProfileUi);
        c.gridy = 2;
        panel.add(applicantProfileButton, c);

        cvLibraryButton = createLaunchButton("Open CV Library UI", this::openCvLibraryUi);
        c.gridy = 3;
        panel.add(cvLibraryButton, c);

        jobPostingButton = createLaunchButton("Open Job Posting UI", this::openJobPostingUi);
        c.gridy = 4;
        panel.add(jobPostingButton, c);

        cvReviewButton = createLaunchButton("Open CV Review UI", this::openCvReviewUi);
        c.gridy = 5;
        panel.add(cvReviewButton, c);

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
        panel.add(new JLabel("Signed-in applicant"), c);

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

        applyButton = createLaunchButton("Open Apply to Job UI", this::openApplyUi);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.insets = new Insets(12, 0, 6, 0);
        panel.add(applyButton, c);

        c.gridy = 4;
        JTextArea helperArea = new JTextArea("""
            Suggested demo values:
            - Sign in as ta001 to use the applicant flow
            - Default jobId: job003

            This launcher now uses the signed-in user instead of manual applicant userId input.
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

    private void openAuthUi() {
        moduleFactory.createAuthUi(this::handleSessionChanged).show();
        setLastActionMessage("Opened Auth UI.");
    }

    private void openApplicantProfileUi() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != UserRole.APPLICANT) {
            setLastActionMessage("Applicant Profile UI requires a signed-in APPLICANT account.");
            return;
        }

        moduleFactory.createUs01Ui(currentUser.get().userId()).show();
        setLastActionMessage("Opened US01 Applicant Profile UI for %s.".formatted(currentUser.get().userId()));
    }

    private void openCvLibraryUi() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != UserRole.APPLICANT) {
            setLastActionMessage("CV Library UI requires a signed-in APPLICANT account.");
            return;
        }

        moduleFactory.createUs02Ui(currentUser.get().userId()).show();
        setLastActionMessage("Opened US02 CV Library UI for %s.".formatted(currentUser.get().userId()));
    }

    private void openJobPostingUi() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != UserRole.MO) {
            setLastActionMessage("Job Posting UI requires a signed-in MO account.");
            return;
        }

        moduleFactory.createJobPostingUi(currentUser.get().userId(), currentUser.get().displayName()).show();
        setLastActionMessage("Opened US11 Job Posting UI for %s.".formatted(currentUser.get().userId()));
    }

    private void openCvReviewUi() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != UserRole.MO) {
            setLastActionMessage("CV Review UI requires a signed-in MO account.");
            return;
        }

        moduleFactory.createCvReviewUi().show();
        setLastActionMessage("Opened CV Review UI for %s.".formatted(currentUser.get().userId()));
    }

    private void openApplyUi() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != UserRole.APPLICANT) {
            setLastActionMessage("Apply to Job UI requires a signed-in APPLICANT account.");
            return;
        }

        String jobId = jobIdField.getText().trim();
        if (jobId.isBlank()) {
            setLastActionMessage("Job ID is required before opening US04.");
            return;
        }

        moduleFactory.createUs04Ui(jobId, currentUser.get().userId()).show();
        setLastActionMessage(
            "Opened US04 Apply UI for applicantUserId=%s and jobId=%s."
                .formatted(currentUser.get().userId(), jobId)
        );
    }

    private void handleSessionChanged() {
        setLastActionMessage("Authentication session updated.");
    }

    private void setLastActionMessage(String message) {
        lastActionMessage = message;
        refreshSessionState();
    }

    private void refreshSessionState() {
        Optional<UserAccount> currentUser = moduleFactory.getCurrentUser();
        boolean applicantSignedIn = currentUser.isPresent() && currentUser.get().role() == UserRole.APPLICANT;
        boolean moSignedIn = currentUser.isPresent() && currentUser.get().role() == UserRole.MO;

        applicantProfileButton.setEnabled(applicantSignedIn);
        cvLibraryButton.setEnabled(applicantSignedIn);
        applyButton.setEnabled(applicantSignedIn);
        jobPostingButton.setEnabled(moSignedIn);
        cvReviewButton.setEnabled(moSignedIn);

        applicantUserIdField.setText(applicantSignedIn ? currentUser.orElseThrow().userId() : "");

        String sessionSummary;
        if (currentUser.isEmpty()) {
            sessionSummary = """
                Current session:
                - not signed in
                - open Auth UI to register or log in
                """;
        } else {
            UserAccount user = currentUser.orElseThrow();
            sessionSummary = """
                Current session:
                - userId: %s
                - role: %s
                - displayName: %s
                - status: %s
                """.formatted(user.userId(), user.role(), user.displayName(), user.status());
        }

        statusArea.setText(sessionSummary + System.lineSeparator() + System.lineSeparator() + lastActionMessage);
    }
}
