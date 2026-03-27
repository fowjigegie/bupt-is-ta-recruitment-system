package com.bupt.tarecruitment.job;

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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Objects;

public final class JobPostingSwingDemo {
    private final JobRepository jobRepository;
    private final JobIdGenerator jobIdGenerator;

    private final JTextField courseTitleField = new JTextField(26);
    private final JTextField taughtByField = new JTextField(18);
    private final JTextField moduleOrActivityField = new JTextField(26);
    private final JTextField weeklyHoursField = new JTextField(8);
    private final JTextField scheduleSlotsField = new JTextField(36);
    private final JTextArea descriptionArea = new JTextArea(6, 46);
    private final JTextArea requirementsArea = new JTextArea(5, 46);
    private final JTextArea resultArea = new JTextArea(9, 46);

    public JobPostingSwingDemo(JobRepository jobRepository, JobIdGenerator jobIdGenerator) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.jobIdGenerator = Objects.requireNonNull(jobIdGenerator);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Job Management - Post Vacancies (Demo UI)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel header = buildHeader();
            JPanel navigation = buildNavigation();

            CardLayout cardLayout = new CardLayout();
            JPanel content = new JPanel(cardLayout);
            content.add(buildNewPostingPanel(), "new-posting");
            content.add(buildPlaceholderPanel("Job Management (coming soon)"), "job-management");
            content.add(buildPlaceholderPanel("Application Review (coming soon)"), "application-review");

            cardLayout.show(content, "new-posting");

            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            root.add(header, BorderLayout.NORTH);
            root.add(navigation, BorderLayout.WEST);
            root.add(content, BorderLayout.CENTER);

            frame.add(root, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            ((JButton) navigation.getClientProperty("btn-post")).addActionListener(e -> cardLayout.show(content, "new-posting"));
            ((JButton) navigation.getClientProperty("btn-manage")).addActionListener(e -> cardLayout.show(content, "job-management"));
            ((JButton) navigation.getClientProperty("btn-review")).addActionListener(e -> cardLayout.show(content, "application-review"));
        });
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel title = new JLabel("BUPT-TA");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.WEST);

        JLabel user = new JLabel("MO Demo");
        user.setForeground(new Color(90, 90, 90));
        panel.add(user, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildNavigation() {
        JPanel nav = new JPanel(new GridBagLayout());
        nav.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(240, 200, 220)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        nav.setBackground(new Color(255, 235, 245));
        nav.setPreferredSize(new Dimension(170, 420));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);

        JButton btnDashboard = new JButton("Dash Board");
        JButton btnPost = new JButton("Post Vacancies");
        JButton btnManage = new JButton("Job Management");
        JButton btnReview = new JButton("Application review");

        btnPost.setBackground(new Color(80, 110, 160));
        btnPost.setForeground(Color.WHITE);

        c.gridy = 0;
        nav.add(btnDashboard, c);
        c.gridy = 1;
        nav.add(btnPost, c);
        c.gridy = 2;
        nav.add(btnManage, c);
        c.gridy = 3;
        nav.add(btnReview, c);

        nav.putClientProperty("btn-post", btnPost);
        nav.putClientProperty("btn-manage", btnManage);
        nav.putClientProperty("btn-review", btnReview);
        return nav;
    }

    private JPanel buildNewPostingPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("New Posting");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(240, 200, 220)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        form.setBackground(new Color(255, 245, 250));

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        requirementsArea.setLineWrap(true);
        requirementsArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setText("""
            Job Posting UI (demo)

            Publish will create a JobPosting and save it into data/jobs.txt.

            Input hints:
            - Job Requirements: use ';' or ',' to separate required skills
            - Schedule slots: use ';' or ',' to separate, e.g. MON-10:00-12:00; THU-14:00-16:00
            - Weekly hours: integer
            """);

        addField(form, 0, "Course Title", courseTitleField);
        addField(form, 1, "Taught By (organiserId)", taughtByField);
        addField(form, 2, "Classes in Need of Assistance (module/activity)", moduleOrActivityField);
        addField(form, 3, "Weekly Hours", weeklyHoursField);
        addField(form, 4, "Schedule Slots", scheduleSlotsField);

        addArea(form, 5, "Job Description", descriptionArea);
        addArea(form, 6, "Job Requirements (skills)", requirementsArea);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        JButton publishButton = new JButton("Publish");
        JButton clearButton = new JButton("Clear");
        buttonRow.setOpaque(false);
        buttonRow.add(publishButton);
        buttonRow.add(clearButton);

        publishButton.addActionListener(e -> publish());
        clearButton.addActionListener(e -> clear());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 6, 0);
        form.add(buttonRow, c);

        panel.add(form, BorderLayout.CENTER);
        panel.add(new JScrollPane(resultArea), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private void publish() {
        try {
            String title = courseTitleField.getText().trim();
            String organiserId = taughtByField.getText().trim();
            String moduleOrActivity = moduleOrActivityField.getText().trim();
            String description = descriptionArea.getText().trim();
            String requirementsRaw = requirementsArea.getText();
            String scheduleRaw = scheduleSlotsField.getText();

            if (title.isBlank()) {
                resultArea.setText("Publish failed.\\n\\nCourse Title is required.");
                return;
            }
            if (organiserId.isBlank()) {
                resultArea.setText("Publish failed.\\n\\nTaught By (organiserId) is required.");
                return;
            }
            if (moduleOrActivity.isBlank()) {
                resultArea.setText("Publish failed.\\n\\nClasses in Need of Assistance (module/activity) is required.");
                return;
            }
            if (description.isBlank()) {
                resultArea.setText("Publish failed.\\n\\nJob Description is required.");
                return;
            }

            int weeklyHours;
            try {
                weeklyHours = Integer.parseInt(weeklyHoursField.getText().trim());
            } catch (NumberFormatException exception) {
                resultArea.setText("Publish failed.\\n\\nWeekly Hours must be an integer.");
                return;
            }
            if (weeklyHours <= 0) {
                resultArea.setText("Publish failed.\\n\\nWeekly Hours must be greater than 0.");
                return;
            }

            String jobId = jobIdGenerator.nextJobId();
            List<String> requiredSkills = splitList(requirementsRaw);
            List<String> scheduleSlots = splitList(scheduleRaw);

            JobPosting posting = new JobPosting(
                jobId,
                organiserId,
                title,
                moduleOrActivity,
                description,
                requiredSkills,
                weeklyHours,
                scheduleSlots,
                JobStatus.OPEN
            );

            jobRepository.save(posting);

            resultArea.setText("""
                Published successfully.

                jobId: %s
                organiserId: %s
                title: %s
                moduleOrActivity: %s
                weeklyHours: %d
                requiredSkills: %s
                scheduleSlots: %s
                status: %s
                """.formatted(
                posting.jobId(),
                posting.organiserId(),
                posting.title(),
                posting.moduleOrActivity(),
                posting.weeklyHours(),
                String.join(", ", posting.requiredSkills()),
                String.join(", ", posting.scheduleSlots()),
                posting.status().name()
            ));
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Publish failed.\\n\\n" + exception.getMessage());
        }
    }

    private void clear() {
        courseTitleField.setText("");
        taughtByField.setText("");
        moduleOrActivityField.setText("");
        weeklyHoursField.setText("");
        scheduleSlotsField.setText("");
        descriptionArea.setText("");
        requirementsArea.setText("");
        resultArea.setText("Cleared.");
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
        labelConstraints.insets = new Insets(6, 6, 6, 12);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(6, 0, 6, 6);

        panel.add(new JLabel(labelText + " :"), labelConstraints);
        panel.add(field, fieldConstraints);
    }

    private void addArea(JPanel panel, int row, String labelText, JTextArea area) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(8, 6, 8, 12);

        GridBagConstraints areaConstraints = new GridBagConstraints();
        areaConstraints.gridx = 1;
        areaConstraints.gridy = row;
        areaConstraints.fill = GridBagConstraints.BOTH;
        areaConstraints.weightx = 1.0;
        areaConstraints.weighty = 1.0;
        areaConstraints.insets = new Insets(8, 0, 8, 6);

        JTextArea box = area;
        box.setBorder(BorderFactory.createLineBorder(new Color(240, 200, 220)));
        panel.add(new JLabel(labelText + " :"), labelConstraints);
        panel.add(new JScrollPane(box), areaConstraints);
    }
}

