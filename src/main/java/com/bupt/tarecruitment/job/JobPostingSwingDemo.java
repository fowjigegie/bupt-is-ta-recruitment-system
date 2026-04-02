package com.bupt.tarecruitment.job;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BoxLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JobPostingSwingDemo {
    private final JobRepository jobRepository;
    private final JobIdGenerator jobIdGenerator;
    private final String fixedOrganiserId;
    private final String headerUserName;

    private final Color selectedNavBg = new Color(80, 110, 160);
    private final Color selectedNavFg = Color.WHITE;

    private Map<JButton, Color> navDefaultBg;
    private Map<JButton, Color> navDefaultFg;
    private JButton navPostButton;
    private JButton navManageButton;
    private JButton navReviewButton;
    private JPanel jobsListPanel;
    private JLabel managementInfoLabel;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private final JTextField courseTitleField = new JTextField(26);
    private final JTextField taughtByField = new JTextField(18);
    private final JTextField moduleOrActivityField = new JTextField(26);
    private final JTextField weeklyHoursField = new JTextField(8);
    private final JTextField scheduleSlotsField = new JTextField(36);
    private final JTextArea descriptionArea = new JTextArea(6, 46);
    private final JTextArea requirementsArea = new JTextArea(5, 46);
    private final JTextArea resultArea = new JTextArea(9, 46);

    public JobPostingSwingDemo(JobRepository jobRepository, JobIdGenerator jobIdGenerator) {
        this(jobRepository, jobIdGenerator, "", "MO Demo");
    }

    public JobPostingSwingDemo(
        JobRepository jobRepository,
        JobIdGenerator jobIdGenerator,
        String fixedOrganiserId,
        String headerUserName
    ) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.jobIdGenerator = Objects.requireNonNull(jobIdGenerator);
        this.fixedOrganiserId = fixedOrganiserId == null ? "" : fixedOrganiserId.trim();
        this.headerUserName = (headerUserName == null || headerUserName.isBlank()) ? "MO Demo" : headerUserName.trim();
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Job Management - Post Vacancies (Demo UI)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout(12, 12));

            JPanel header = buildHeader();
            JPanel navigation = buildNavigation();

            cardLayout = new CardLayout();
            contentPanel = new JPanel(cardLayout);
            contentPanel.add(buildDashboardPanel(), "dashboard");
            contentPanel.add(buildNewPostingPanel(), "new-posting");
            contentPanel.add(buildJobManagementPanel(), "job-management");
            contentPanel.add(buildPlaceholderPanel("Application Review (coming soon)"), "application-review");

            cardLayout.show(contentPanel, "dashboard");

            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            root.add(header, BorderLayout.NORTH);
            root.add(navigation, BorderLayout.WEST);
            root.add(contentPanel, BorderLayout.CENTER);

            frame.add(root, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JButton btnDashboard = (JButton) navigation.getClientProperty("btn-dashboard");
            JButton btnPost = (JButton) navigation.getClientProperty("btn-post");
            JButton btnManage = (JButton) navigation.getClientProperty("btn-manage");
            JButton btnReview = (JButton) navigation.getClientProperty("btn-review");

            btnDashboard.addActionListener(e -> {
                applyNavigationSelected(btnDashboard);
                cardLayout.show(contentPanel, "dashboard");
                refreshDashboardList();
            });
            btnPost.addActionListener(e -> {
                applyNavigationSelected(btnPost);
                cardLayout.show(contentPanel, "new-posting");
            });
            btnManage.addActionListener(e -> {
                applyNavigationSelected(btnManage);
                cardLayout.show(contentPanel, "job-management");
            });
            btnReview.addActionListener(e -> {
                applyNavigationSelected(btnReview);
                cardLayout.show(contentPanel, "application-review");
            });

            // default selected = dashboard
            applyNavigationSelected(btnDashboard);
            refreshDashboardList();
        });
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel title = new JLabel("BUPT-TA");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.WEST);

        JLabel user = new JLabel("MO Demo");
        user.setText(headerUserName);
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

        navPostButton = btnPost;
        navManageButton = btnManage;
        navReviewButton = btnReview;

        c.gridy = 0;
        nav.add(btnDashboard, c);
        c.gridy = 1;
        nav.add(btnPost, c);
        c.gridy = 2;
        nav.add(btnManage, c);
        c.gridy = 3;
        nav.add(btnReview, c);

        navDefaultBg = new HashMap<>();
        navDefaultFg = new HashMap<>();
        for (JButton button : List.of(btnDashboard, btnPost, btnManage, btnReview)) {
            navDefaultBg.put(button, button.getBackground());
            navDefaultFg.put(button, button.getForeground());
            button.setOpaque(true);
        }

        nav.putClientProperty("btn-post", btnPost);
        nav.putClientProperty("btn-manage", btnManage);
        nav.putClientProperty("btn-review", btnReview);
        nav.putClientProperty("btn-dashboard", btnDashboard);
        return nav;
    }

    private void applyNavigationSelected(JButton selected) {
        if (navDefaultBg == null || navDefaultFg == null) {
            return;
        }

        for (JButton button : navDefaultBg.keySet()) {
            boolean isSelected = button == selected;
            button.setBackground(isSelected ? selectedNavBg : navDefaultBg.get(button));
            button.setForeground(isSelected ? selectedNavFg : navDefaultFg.get(button));
        }
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel welcome = new JLabel("Welcome Back", SwingConstants.CENTER);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 36f));
        welcome.setForeground(new Color(60, 90, 160));

        JPanel shortcutsRow = buildDashboardShortcutsRow();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(welcome, BorderLayout.NORTH);
        topPanel.add(shortcutsRow, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        JLabel listTitle = new JLabel("Jobs I have posted");
        listTitle.setFont(listTitle.getFont().deriveFont(Font.BOLD, 14f));
        listTitle.setHorizontalAlignment(SwingConstants.LEFT);

        jobsListPanel = new JPanel();
        jobsListPanel.setLayout(new BoxLayout(jobsListPanel, BoxLayout.Y_AXIS));

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(listTitle, BorderLayout.NORTH);
        center.add(new JScrollPane(jobsListPanel), BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);
        panel.add(new JLabel("No further information ...", SwingConstants.CENTER), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildDashboardShortcutsRow() {
        JButton postShortcut = createRoundedShortcutButton(
            "Post a new vacancy",
            null,
            new Color(245, 179, 199)
        );

        JButton pendingShortcut = createRoundedShortcutButton(
            "Pending\napplications",
            String.valueOf(countPendingApplications()),
            new Color(245, 179, 199)
        );

        JButton chatShortcut = createRoundedShortcutButton(
            "Chat",
            "3",
            new Color(245, 179, 199)
        );

        postShortcut.addActionListener(e -> {
            applyNavigationSelected(navPostButton);
            cardLayout.show(contentPanel, "new-posting");
        });

        pendingShortcut.addActionListener(e -> {
            applyNavigationSelected(navReviewButton);
            cardLayout.show(contentPanel, "application-review");
        });

        chatShortcut.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                null,
                "Chat UI is coming soon.",
                "Not implemented",
                JOptionPane.INFORMATION_MESSAGE
            );
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        row.setOpaque(false);
        row.add(postShortcut);
        row.add(pendingShortcut);
        row.add(chatShortcut);
        return row;
    }

    private int countPendingApplications() {
        try {
            // Reuse the app repository only if it exists via default constructor path.
            // For now, keep it simple: count SUBMITTED records from the file-based repository.
            var repo = new com.bupt.tarecruitment.application.TextFileApplicationRepository();
            long pendingCount = repo.findAll().stream()
                .filter(app -> app.status() == com.bupt.tarecruitment.application.ApplicationStatus.SUBMITTED)
                .count();

            return pendingCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pendingCount;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private JButton createRoundedShortcutButton(String text, String badgeText, Color bg) {
        RoundedShortcutButton btn = new RoundedShortcutButton(text, bg);
        btn.setPreferredSize(new Dimension(240, 80));

        if (badgeText != null) {
            btn.setBadgeText(badgeText);
        }

        return btn;
    }

    private static final class RoundedShortcutButton extends JButton {
        private final Color fill;
        private String badgeText;

        RoundedShortcutButton(String text, Color fill) {
            super(text);
            this.fill = fill;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        void setBadgeText(String badgeText) {
            this.badgeText = badgeText;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 26, 26);
                g2.setColor(fill);
                g2.fill(rect);

                super.paintComponent(g);

                if (badgeText != null && !badgeText.isBlank()) {
                    int bw = 28;
                    int bh = 28;
                    int x = w - bw / 2 - 8;
                    int y = -10;

                    g2.setColor(new Color(229, 57, 53));
                    g2.fillOval(x, y, bw, bh);
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                    // Simple centered text (approx)
                    g2.drawString(badgeText, x + 8, y + 19);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private void refreshDashboardList() {
        if (jobsListPanel == null) {
            return;
        }

        jobsListPanel.removeAll();

        List<JobPosting> jobs = jobRepository.findAll();
        if (jobs.isEmpty()) {
            JLabel empty = new JLabel("(no jobs found in data/jobs.txt)");
            jobsListPanel.add(empty);
        } else {
            for (JobPosting job : jobs) {
                jobsListPanel.add(buildJobRow(job));
            }
        }

        jobsListPanel.revalidate();
        jobsListPanel.repaint();
    }

    private JPanel buildJobRow(JobPosting job) {
        String statusText = job.status() == JobStatus.OPEN ? "Open" : "Closed";

        JPanel row = new JPanel(new BorderLayout(12, 12));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(240, 120, 170)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.setBackground(Color.WHITE);

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);

        JLabel headline = new JLabel("%s - %s".formatted(job.moduleOrActivity(), job.title()));
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 14f));

        JLabel meta = new JLabel("Status: %s".formatted(statusText));
        meta.setForeground(new Color(70, 70, 70));

        text.add(headline, BorderLayout.NORTH);
        text.add(meta, BorderLayout.SOUTH);

        JButton manageButton = new JButton("Managing This Job");
        manageButton.addActionListener(e -> {
            if (managementInfoLabel != null) {
                managementInfoLabel.setText("Managing job: " + job.jobId() + " (" + statusText + ")");
            }
            applyNavigationSelected(navManageButton);
            cardLayout.show(contentPanel, "job-management");
        });

        row.add(text, BorderLayout.CENTER);
        row.add(manageButton, BorderLayout.EAST);
        return row;
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
            Publish will create a JobPosting and save it into data/jobs.txt.

            Input hints:
            - Job Requirements (skills): use ';' or ',' to separate
            - Schedule slots: use ';' or ',' to separate, e.g. MON-10:00-12:00; THU-14:00-16:00
            - Weekly hours: integer
            """);

        if (!fixedOrganiserId.isBlank()) {
            taughtByField.setText(fixedOrganiserId);
            taughtByField.setEditable(false);
        }

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

    private JPanel buildJobManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Job Management");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(title, BorderLayout.NORTH);

        managementInfoLabel = new JLabel("Select a job in Dashboard to manage (stub).");
        managementInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(managementInfoLabel, BorderLayout.CENTER);
        panel.add(new JLabel("This part is coming soon.", SwingConstants.CENTER), BorderLayout.SOUTH);

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

            refreshDashboardList();
        } catch (IllegalArgumentException exception) {
            resultArea.setText("Publish failed.\\n\\n" + exception.getMessage());
        }
    }

    private void clear() {
        courseTitleField.setText("");
        taughtByField.setText(fixedOrganiserId);
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

