package com.bupt.tarecruitment.auth;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

public final class AuthSwingDemo {
    private final AuthService service;
    private final Runnable onSessionChanged;

    private final JTextField userIdField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JComboBox<UserRole> roleComboBox = new JComboBox<>(UserRole.values());
    private final JTextArea resultArea = new JTextArea(12, 48);

    public AuthSwingDemo(AuthService service) {
        this(service, () -> { });
    }

    public AuthSwingDemo(AuthService service, Runnable onSessionChanged) {
        this.service = Objects.requireNonNull(service);
        this.onSessionChanged = Objects.requireNonNull(onSessionChanged);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("US-00 Authentication UI");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            addField(formPanel, 0, "User ID", userIdField);
            addField(formPanel, 1, "Password", passwordField);
            addField(formPanel, 2, "Role", roleComboBox);

            JPanel buttonPanel = new JPanel();
            JButton registerButton = new JButton("Register");
            JButton loginButton = new JButton("Login");
            JButton currentUserButton = new JButton("View Current User");
            JButton logoutButton = new JButton("Logout");
            JButton clearButton = new JButton("Clear");

            buttonPanel.add(registerButton);
            buttonPanel.add(loginButton);
            buttonPanel.add(currentUserButton);
            buttonPanel.add(logoutButton);
            buttonPanel.add(clearButton);

            resultArea.setEditable(false);
            resultArea.setLineWrap(true);
            resultArea.setWrapStyleWord(true);
            resultArea.setText("""
                US-00 Authentication UI

                Notes:
                - APPLICANT userId should look like ta...
                - MO userId should look like mo...
                - ADMIN userId should look like admin...
                - This UI uses the same users.txt storage as the console workflow.
                """);

            registerButton.addActionListener(event -> register());
            loginButton.addActionListener(event -> login());
            currentUserButton.addActionListener(event -> viewCurrentUser());
            logoutButton.addActionListener(event -> logout());
            clearButton.addActionListener(event -> clearForm());

            frame.add(formPanel, BorderLayout.NORTH);
            frame.add(buttonPanel, BorderLayout.CENTER);
            frame.add(new JScrollPane(resultArea), BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private void register() {
        try {
            UserAccount account = service.register(
                userIdField.getText().trim(),
                new String(passwordField.getPassword()),
                (UserRole) roleComboBox.getSelectedItem()
            );
            renderAccount("Registration successful.", account);
            onSessionChanged.run();
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to register user.\n\n" + exception.getMessage());
        }
    }

    private void login() {
        try {
            UserAccount account = service.login(
                userIdField.getText().trim(),
                new String(passwordField.getPassword())
            );
            resultArea.setText("""
                Login successful.

                userId: %s
                role: %s
                displayName: %s
                status: %s
                """.formatted(
                account.userId(),
                account.role(),
                account.displayName(),
                account.status()
            ));
            onSessionChanged.run();
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Failed to log in.\n\n" + exception.getMessage());
        }
    }

    private void viewCurrentUser() {
        service.getCurrentUser()
            .ifPresentOrElse(
                account -> renderAccount("Current user loaded.", account),
                () -> resultArea.setText("No user is currently logged in.")
            );
    }

    private void logout() {
        service.logout();
        resultArea.setText("Logged out.");
        onSessionChanged.run();
    }

    private void clearForm() {
        userIdField.setText("");
        passwordField.setText("");
        roleComboBox.setSelectedItem(UserRole.APPLICANT);
        resultArea.setText("Form cleared.");
    }

    private void renderAccount(String title, UserAccount account) {
        resultArea.setText("""
            %s

            userId: %s
            role: %s
            displayName: %s
            status: %s
            """.formatted(
            title,
            account.userId(),
            account.role(),
            account.displayName(),
            account.status()
        ));
    }

    private void addField(JPanel panel, int row, String labelText, Component component) {
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
        panel.add(component, fieldConstraints);
    }
}
