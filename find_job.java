import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * BUPT-TA — TA side: browse open TA positions (More Jobs).
 */
public class find_job extends JFrame {

    private static final int PAGE_SIZE = 5;
    private static final Color HEADER_START = new Color(255, 245, 200);
    private static final Color HEADER_END = new Color(255, 220, 230);
    private static final Color SIDEBAR_BG = new Color(255, 180, 200);
    /** Dark selection: sidebar active item & current pagination (white text) */
    private static final Color SELECTED_DARK = new Color(40, 60, 120);
    private static final Color SEARCH_BG = new Color(160, 120, 200);
    private static final Color BTN_PINK = new Color(255, 150, 180);
    private static final Color BTN_PINK_DARK = new Color(220, 100, 140);

    private final List<JobPosting> allJobs;
    private List<JobPosting> filtered;
    private int currentPage = 0;
    /** false = newest first (descending by time) */
    private boolean sortAscending = false;

    private JTextField searchField;
    private JButton timeButton;
    private JPanel listPanel;
    private JPanel paginationPanel;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** Per-job chat lines for demo */
    private final Map<Integer, StringBuilder> chatLogs = new ConcurrentHashMap<>();
    /** Job id whose chat window is open; MO messages do not increment unread for this id. */
    private int openChatJobId = -1;

    public find_job() {
        super("BUPT-TA — More Jobs");
        allJobs = buildSampleJobs();
        filtered = new ArrayList<>(allJobs);
        applySortAndFilter();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createSidebar(null), BorderLayout.WEST);
        root.add(createMainContent(), BorderLayout.CENTER);

        setContentPane(root);

        // Demo: MO occasionally sends a message; unread badge appears only then.
        new Timer(12000, e -> simulateIncomingMoMessage()).start();
    }

    /** Simulates the recruiter (MO) sending a new message; TA sees a red dot + count until opening chat. */
    private void simulateIncomingMoMessage() {
        if (allJobs.isEmpty()) {
            return;
        }
        JobPosting job = allJobs.get((int) (Math.random() * allJobs.size()));
        if (job.id == openChatJobId) {
            return;
        }
        String[] lines = {
                "Could you confirm your availability for this week?",
                "Please check the updated lab schedule.",
                "Are you able to join the briefing on Friday?",
                "We have one more slot — let me know if you are interested.",
                "Thanks for your interest; when can we have a short call?"
        };
        String line = lines[(int) (Math.random() * lines.length)];
        StringBuilder log = chatLogs.computeIfAbsent(job.id, k -> new StringBuilder());
        log.append("[").append(job.moName).append("]: ").append(line).append("\n");
        job.unreadCount++;
        if (job.chatStatus == ChatStatus.NEW_CHAT) {
            job.chatStatus = ChatStatus.HAS_HISTORY;
        }
        SwingUtilities.invokeLater(this::refresh);
    }

    private static List<JobPosting> buildSampleJobs() {
        List<JobPosting> list = new ArrayList<>();
        int id = 1;
        list.add(new JobPosting(id++, "EBUS204", "Operating Systems TA", "Farther",
                LocalDateTime.of(2026, 3, 20, 10, 30),
                "Assist with labs, office hours, and assignment grading for Operating Systems.",
                "You are required to complete the summary of all tasks on time, ensuring accuracy and proper formatting, and synchronizing progress in a timely manner.",
                "Farther", "2024215101 ~ 2024215104",
                ChatStatus.NEW_CHAT, 0));
        list.add(new JobPosting(id++, "CBUS201", "Machine Learning TA", "Kina",
                LocalDateTime.of(2026, 3, 18, 14, 0),
                "Help with tutorial sessions and project consultations.",
                "You are required to attend weekly syncs, maintain grading rubrics, and report issues promptly.",
                "Kina", "2024215105 ~ 2024215108",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "CS101", "Introduction to Programming TA", "Farther",
                LocalDateTime.of(2026, 3, 22, 9, 15),
                "Weekly lab support and beginner-friendly debugging help.",
                "You are required to prepare lab materials, help beginners, and log attendance each week.",
                "Farther", "2024215109 ~ 2024215112",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "NET302", "Computer Networks TA", "Wei",
                LocalDateTime.of(2026, 3, 15, 16, 45),
                "Grade homework and hold Q&A for protocol assignments.",
                "You are required to follow the marking scheme strictly and return grades within five working days.",
                "Wei", "2024215113 ~ 2024215116",
                ChatStatus.NEW_CHAT, 0));
        list.add(new JobPosting(id++, "DBMS210", "Database Systems TA", "Kina",
                LocalDateTime.of(2026, 3, 25, 11, 0),
                "SQL labs and exam review sessions.",
                "You are required to complete the summary of all tasks on time, ensuring the accuracy and proper formatting of the data, and synchronizing the progress in a timely manner.",
                "Kina", "2024215117 ~ 2024215120",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "SE301", "Software Engineering TA", "Chen",
                LocalDateTime.of(2026, 3, 10, 8, 30),
                "Support team projects and documentation reviews.",
                "You are required to monitor group progress, review deliverables, and provide written feedback.",
                "Chen", "2024215121 ~ 2024215124",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "AI405", "Artificial Intelligence TA", "Wei",
                LocalDateTime.of(2026, 3, 28, 13, 20),
                "Tutorial for search algorithms and ML basics.",
                "You are required to run tutorials, prepare exercises, and assist with coursework questions.",
                "Wei", "2024215125 ~ 2024215128",
                ChatStatus.NEW_CHAT, 0));
        list.add(new JobPosting(id++, "WEB220", "Web Development TA", "Chen",
                LocalDateTime.of(2026, 3, 12, 15, 0),
                "Front-end labs and code review.",
                "You are required to review student code fairly, use the provided checklist, and document common mistakes.",
                "Chen", "2024215129 ~ 2024215132",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "COMP304", "Compilers TA", "Farther",
                LocalDateTime.of(2026, 3, 19, 10, 0),
                "Parser and codegen assignment help.",
                "You are required to hold office hours, debug toolchain issues, and escalate blockers to the MO.",
                "Farther", "2024215133 ~ 2024215136",
                ChatStatus.HAS_HISTORY, 0));
        list.add(new JobPosting(id++, "SEC401", "Information Security TA", "Kina",
                LocalDateTime.of(2026, 3, 27, 17, 30),
                "Crypto exercises and secure coding workshops.",
                "You are required to enforce lab safety rules, proctor quizzes, and handle make-up sessions as needed.",
                "Kina", "2024215137 ~ 2024215140",
                ChatStatus.NEW_CHAT, 0));
        list.add(new JobPosting(id++, "MOB330", "Mobile Computing TA", "Wei",
                LocalDateTime.of(2026, 3, 14, 12, 0),
                "Android lab assistance.",
                "You are required to test lab devices before class, assist with emulators, and collect feedback after each session.",
                "Wei", "2024215141 ~ 2024215144",
                ChatStatus.NEW_CHAT, 0));

        return list;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, HEADER_START, getWidth(), 0, HEADER_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 56));
        header.setOpaque(false);

        JLabel brand = new JLabel("BUPT-TA");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 22f));
        brand.setBorder(new EmptyBorder(0, 16, 0, 0));

        JLabel user = new JLabel("<html><div style='text-align:right'>LI HUA<br/><span style='font-size:11px'>computer department</span></div></html>");
        user.setHorizontalAlignment(SwingConstants.RIGHT);
        user.setBorder(new EmptyBorder(4, 0, 4, 8));

        JLabel star = new JLabel("\u2605");
        star.setFont(star.getFont().deriveFont(20f));
        star.setForeground(new Color(200, 160, 80));
        star.setBorder(new EmptyBorder(0, 0, 0, 16));

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 8));
        east.setOpaque(false);
        east.add(user);
        east.add(star);

        header.add(brand, BorderLayout.WEST);
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /**
     * @param onLeaveChat if non-null (e.g. chat dialog), "More Jobs" runs this (e.g. close dialog).
     */
    private JPanel createSidebar(Runnable onLeaveChat) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(180, 0));
        side.setBorder(new EmptyBorder(16, 8, 16, 8));

        String[] labels = {"Dash Board", "More Jobs", "Resume Database", "application status"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            JButton b = new JButton(labels[i]);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
            b.setFocusPainted(false);
            b.setUI(new BasicButtonUI());
            if (i == 1) {
                styleSelectedButton(b, SELECTED_DARK);
            } else {
                b.setOpaque(true);
                b.setContentAreaFilled(true);
                b.setBorderPainted(false);
                b.setBackground(SIDEBAR_BG);
                b.setForeground(Color.BLACK);
            }
            b.addActionListener(e -> {
                if (idx == 1 && onLeaveChat != null) {
                    onLeaveChat.run();
                    return;
                }
                if (idx != 1) {
                    JOptionPane.showMessageDialog(find_job.this,
                            "This section is not implemented in the demo.",
                            "BUPT-TA", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }
        return side;
    }

    /** Dark background + white text; works with system LAF when paired with {@link BasicButtonUI}. */
    private static void styleSelectedButton(AbstractButton b, Color darkBg) {
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setBackground(darkBg);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkBg.darker(), 1),
                new EmptyBorder(8, 12, 8, 12)));
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);

        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEARCH_BG.darker(), 1),
                new EmptyBorder(8, 12, 8, 12)));
        searchField.setBackground(new Color(245, 235, 255));
        final String searchHint = "Course id / Course Name / MO Name ...";
        searchField.setForeground(Color.GRAY);
        searchField.setText(searchHint);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchHint.equals(searchField.getText())) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(searchHint);
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        JLabel searchIcon = new JLabel("\u2315 ");
        searchIcon.setFont(searchIcon.getFont().deriveFont(18f));
        JPanel searchWrap = new JPanel(new BorderLayout(4, 0));
        searchWrap.setBackground(SEARCH_BG);
        searchWrap.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        timeButton = new JButton("Time \u2195");
        stylePinkButton(timeButton);
        timeButton.addActionListener(e -> toggleSort());

        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timeRow.setOpaque(false);
        timeRow.add(timeButton);
        updateTimeButtonLabel();

        top.add(searchWrap, BorderLayout.NORTH);
        top.add(timeRow, BorderLayout.SOUTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JButton back = new JButton("\u2190");
        back.setFont(back.getFont().deriveFont(18f));
        back.setForeground(SEARCH_BG.darker());
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.addActionListener(e -> dispose());

        paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        paginationPanel.setOpaque(false);

        bottom.add(back, BorderLayout.WEST);
        bottom.add(paginationPanel, BorderLayout.EAST);

        main.add(top, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });

        refresh();
        return main;
    }

    private void toggleSort() {
        sortAscending = !sortAscending;
        updateTimeButtonLabel();
        refresh();
    }

    private void updateTimeButtonLabel() {
        timeButton.setText(sortAscending ? "Time \u2191" : "Time \u2193");
    }

    private void applySortAndFilter() {
        String raw = searchField != null ? searchField.getText() : "";
        String hint = "Course id / Course Name / MO Name ...";
        String q = hint.equals(raw) ? "" : raw.trim();
        String ql = q.toLowerCase();
        filtered = new ArrayList<>();
        for (JobPosting j : allJobs) {
            if (ql.isEmpty()
                    || j.courseId.toLowerCase().contains(ql)
                    || j.courseTitle.toLowerCase().contains(ql)
                    || j.moName.toLowerCase().contains(ql)) {
                filtered.add(j);
            }
        }
        Comparator<JobPosting> cmp = Comparator.comparing(j -> j.postTime);
        if (!sortAscending) {
            cmp = cmp.reversed();
        }
        filtered.sort(cmp);
    }

    private int totalPages() {
        if (filtered.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) filtered.size() / PAGE_SIZE);
    }

    private void refresh() {
        applySortAndFilter();
        int tp = totalPages();
        if (currentPage >= tp) {
            currentPage = Math.max(0, tp - 1);
        }
        rebuildList();
        rebuildPagination();
        revalidate();
        repaint();
    }

    private void rebuildList() {
        listPanel.removeAll();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("Nothing Found (>﹏<)");
            empty.setBorder(new EmptyBorder(24, 8, 8, 8));
            listPanel.add(empty);
        } else {
            for (int i = start; i < end; i++) {
                JobPosting job = filtered.get(i);
                listPanel.add(createJobRow(job));
                if (i < end - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, sep.getPreferredSize().height));
                    listPanel.add(sep);
                }
            }
        }
    }

    private JPanel createJobRow(JobPosting job) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(12, 8, 12, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel line1 = new JLabel(job.courseId + " - " + job.courseTitle);
        line1.setFont(line1.getFont().deriveFont(Font.BOLD, 15f));
        JLabel line2 = new JLabel("MO: " + job.moName);
        line2.setForeground(new Color(80, 80, 80));
        JPanel west = new JPanel();
        west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
        west.setOpaque(false);
        west.add(line1);
        west.add(Box.createVerticalStrut(4));
        west.add(line2);

        JButton details = new JButton("View Details");
        stylePinkButton(details);
        details.addActionListener(e -> showDetails(job));

        boolean hasChatHistory = job.chatStatus == ChatStatus.HAS_HISTORY
                || (chatLogs.containsKey(job.id) && chatLogs.get(job.id).length() > 0);
        String chatLabel = hasChatHistory ? "View Chat History" : "Chat with MO";
        JButton chatBtn = new JButton(chatLabel);
        stylePinkButton(chatBtn);
        chatBtn.addActionListener(e -> openChat(job));

        JPanel chatWrap = wrapChatButtonWithBadge(chatBtn, job.unreadCount);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        east.setOpaque(false);
        east.add(details);
        east.add(chatWrap);

        row.add(west, BorderLayout.WEST);
        row.add(east, BorderLayout.EAST);
        return row;
    }

    private static void stylePinkButton(AbstractButton b) {
        b.setBackground(BTN_PINK);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_PINK_DARK, 1),
                new EmptyBorder(6, 12, 6, 12)));
    }

    /** Red circular badge (top-right of chat button) showing unread count from MO. */
    private static JPanel createUnreadDotBadge(int unread) {
        final String text = unread > 9 ? "9+" : String.valueOf(unread);
        return new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Font f = getFont().deriveFont(Font.BOLD, 12f);
                FontMetrics fm = getFontMetrics(f);
                int w = Math.max(22, fm.stringWidth(text) + 10);
                int h = 22;
                return new Dimension(w, h);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.RED);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
    }

    private static JPanel wrapChatButtonWithBadge(JButton chatBtn, int unread) {
        if (unread <= 0) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            p.setOpaque(false);
            p.add(chatBtn);
            return p;
        }
        Dimension bd = chatBtn.getPreferredSize();
        chatBtn.setSize(bd);
        int padTop = 6;
        JPanel badge = createUnreadDotBadge(unread);
        Dimension bs = badge.getPreferredSize();
        int lpW = bd.width + 6;
        int lpH = bd.height + padTop;
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(lpW, lpH));
        layered.setMinimumSize(new Dimension(lpW, lpH));
        chatBtn.setBounds(0, padTop, bd.width, bd.height);
        layered.add(chatBtn, JLayeredPane.DEFAULT_LAYER);
        int bx = lpW - (int) bs.getWidth() + 2;
        badge.setBounds(bx, 0, (int) bs.getWidth(), (int) bs.getHeight());
        layered.add(badge, JLayeredPane.PALETTE_LAYER);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(layered);
        return wrap;
    }

    private static String htmlEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatCourseTitlePill(JobPosting job) {
        String shortTitle = job.courseTitle.replace(" TA", "").trim().replace(' ', '-');
        return job.courseId + "-" + shortTitle;
    }

    /** Rounded light-pink pill with white text (detail page). */
    private static JPanel createPill(String text) {
        JLabel l = new JLabel("<html><div style='color:#ffffff;text-align:center'>"
                + htmlEscape(text) + "</div></html>");
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new EmptyBorder(12, 18, 12, 18));
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 175, 195));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static JPanel createDetailTextBox(String label, String body) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 155, 180), 3),
                new EmptyBorder(18, 20, 18, 20)));
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setFont(ta.getFont().deriveFont(15f));
        ta.setText(label + " " + body);
        ta.setBorder(null);
        wrap.add(ta, BorderLayout.CENTER);
        return wrap;
    }

    /** Header for detail dialog: bell, divider, blue profile text, avatar (mockup). */
    private JPanel createDetailHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, HEADER_START, getWidth(), 0, HEADER_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 64));
        header.setOpaque(false);

        JLabel brand = new JLabel("BUPT-TA");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 22f));
        brand.setForeground(Color.BLACK);
        brand.setBorder(new EmptyBorder(0, 16, 0, 0));

        JLabel bell = new JLabel("\uD83D\uDD14");
        bell.setFont(bell.getFont().deriveFont(20f));
        bell.setForeground(new Color(220, 60, 60));

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(8, 28));
        sep.setForeground(new Color(180, 180, 200));

        Color blue = new Color(35, 55, 130);
        JLabel user = new JLabel("<html><div style='text-align:right'><b><span style='color:rgb(35,55,130)'>LI HUA</span></b><br/>"
                + "<span style='font-size:12px;color:rgb(35,55,130)'>computer department</span></div></html>");
        user.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel avatar = new JPanel(new BorderLayout());
        avatar.setPreferredSize(new Dimension(40, 40));
        avatar.setMinimumSize(new Dimension(40, 40));
        avatar.setBackground(SEARCH_BG);
        avatar.setBorder(BorderFactory.createLineBorder(SEARCH_BG.darker(), 1));
        JLabel star = new JLabel("\u2605", SwingConstants.CENTER);
        star.setForeground(Color.WHITE);
        star.setFont(star.getFont().deriveFont(18f));
        avatar.add(star, BorderLayout.CENTER);

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        east.setOpaque(false);
        east.add(bell);
        east.add(sep);
        east.add(user);
        east.add(avatar);

        header.add(brand, BorderLayout.WEST);
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /** Sidebar for detail dialog: inactive items use white text on pink (mockup). */
    private JPanel createSidebarDetail(Runnable onClose) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(180, 0));
        side.setBorder(new EmptyBorder(16, 8, 16, 8));

        String[] labels = {"Dash Board", "More Jobs", "Resume Database", "application status"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            JButton b = new JButton(labels[i]);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
            b.setFocusPainted(false);
            b.setUI(new BasicButtonUI());
            if (i == 1) {
                styleSelectedButton(b, SELECTED_DARK);
            } else {
                b.setOpaque(true);
                b.setContentAreaFilled(true);
                b.setBorderPainted(false);
                b.setBackground(SIDEBAR_BG);
                b.setForeground(Color.WHITE);
            }
            b.addActionListener(e -> {
                if (idx == 1 && onClose != null) {
                    onClose.run();
                    return;
                }
                if (idx != 1) {
                    JOptionPane.showMessageDialog(find_job.this,
                            "This section is not implemented in the demo.",
                            "BUPT-TA", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }
        return side;
    }

    private void showDetails(JobPosting job) {
        final JDialog dlg = new JDialog(this, "BUPT-TA — Job details", true);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout());
        dlg.add(createDetailHeader(), BorderLayout.NORTH);
        dlg.add(createSidebarDetail(dlg::dispose), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 8));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row1.add(createPill("Course Title: " + formatCourseTitlePill(job)));
        row1.add(createPill("Taught By: " + job.taughtBy));

        content.add(row1);
        content.add(Box.createVerticalStrut(12));

        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);
        row2.add(createPill("Classes in Need of Assistance: " + job.classesRange), BorderLayout.CENTER);
        content.add(row2);
        content.add(Box.createVerticalStrut(16));

        content.add(createDetailTextBox("Job Description :", job.description));
        content.add(Box.createVerticalStrut(14));
        content.add(createDetailTextBox("Job Requirements :", job.jobRequirements));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton back = new JButton("\u2190");
        back.setFont(back.getFont().deriveFont(22f));
        back.setForeground(SEARCH_BG.darker());
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.addActionListener(e -> dlg.dispose());
        bottom.add(back, BorderLayout.WEST);

        main.add(scroll, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        dlg.add(main, BorderLayout.CENTER);

        dlg.setMinimumSize(getMinimumSize());
        dlg.setSize(getWidth(), getHeight());
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void openChat(JobPosting job) {
        openChatJobId = job.id;
        job.unreadCount = 0;

        final JDialog dlg = new JDialog(this, "BUPT-TA — Chat", true);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (openChatJobId == job.id) {
                    openChatJobId = -1;
                }
            }
        });
        dlg.setLayout(new BorderLayout());
        dlg.add(createHeader(), BorderLayout.NORTH);
        dlg.add(createSidebar(dlg::dispose), BorderLayout.WEST);

        JPanel chatMain = new JPanel(new BorderLayout());
        chatMain.setBackground(Color.WHITE);
        chatMain.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel titleStrip = new JPanel(new BorderLayout(8, 0));
        titleStrip.setBackground(SEARCH_BG);
        titleStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEARCH_BG.darker(), 1),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel chatHeadline = new JLabel("<html><b>Chat with MO</b> — " + job.moName
                + "<br/><span style='font-weight:normal;font-size:12px'>" + job.courseId + " - " + job.courseTitle + "</span></html>");
        chatHeadline.setForeground(Color.BLACK);
        titleStrip.add(chatHeadline, BorderLayout.CENTER);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(area.getFont().deriveFont(13f));
        area.setBackground(Color.WHITE);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        StringBuilder log = chatLogs.computeIfAbsent(job.id, k -> new StringBuilder());
        area.setText(log.toString());
        area.setCaretPosition(area.getDocument().getLength());

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JTextField input = new JTextField();
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SEARCH_BG.darker(), 1),
                new EmptyBorder(8, 12, 8, 12)));
        input.setBackground(new Color(245, 235, 255));

        JButton send = new JButton("Send");
        stylePinkButton(send);
        ActionListener sendMessage = ev -> {
            String t = input.getText().trim();
            if (t.isEmpty()) {
                return;
            }
            log.append("[You]: ").append(t).append("\n");
            input.setText("");
            job.chatStatus = ChatStatus.HAS_HISTORY;
            log.append("[").append(job.moName).append("]: Thanks for your message. We'll get back to you soon.\n");
            area.setText(log.toString());
            area.setCaretPosition(area.getDocument().getLength());
        };
        send.addActionListener(sendMessage);
        input.addActionListener(sendMessage);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        JButton backChat = new JButton("\u2190");
        backChat.setFont(backChat.getFont().deriveFont(18f));
        backChat.setForeground(SEARCH_BG.darker());
        backChat.setContentAreaFilled(false);
        backChat.setBorderPainted(false);
        backChat.addActionListener(e -> dlg.dispose());

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(send, BorderLayout.EAST);

        bottom.add(backChat, BorderLayout.WEST);
        bottom.add(inputRow, BorderLayout.CENTER);

        chatMain.add(titleStrip, BorderLayout.NORTH);
        chatMain.add(scroll, BorderLayout.CENTER);
        chatMain.add(bottom, BorderLayout.SOUTH);

        dlg.add(chatMain, BorderLayout.CENTER);

        dlg.setMinimumSize(getMinimumSize());
        dlg.setSize(getWidth(), getHeight());
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        refresh();
    }

    private void rebuildPagination() {
        paginationPanel.removeAll();
        int tp = totalPages();
        for (int p = 0; p < tp; p++) {
            final int page = p;
            JButton pb = new JButton(String.valueOf(p + 1));
            pb.setUI(new BasicButtonUI());
            if (p == currentPage) {
                styleSelectedButton(pb, SELECTED_DARK);
            } else {
                pb.setOpaque(true);
                pb.setContentAreaFilled(true);
                pb.setBorderPainted(true);
                pb.setBackground(BTN_PINK);
                pb.setForeground(Color.BLACK);
                pb.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BTN_PINK_DARK, 1),
                        new EmptyBorder(6, 12, 6, 12)));
            }
            pb.setFocusPainted(false);
            pb.addActionListener(e -> {
                currentPage = page;
                refresh();
            });
            paginationPanel.add(pb);
        }
    }

    enum ChatStatus {
        NEW_CHAT,
        HAS_HISTORY
    }

    static class JobPosting {
        final int id;
        final String courseId;
        final String courseTitle;
        final String moName;
        final LocalDateTime postTime;
        /** Short job description (detail page). */
        final String description;
        /** Longer requirements text (detail page). */
        final String jobRequirements;
        /** Instructors name for "Taught By" pill (often same as MO). */
        final String taughtBy;
        /** e.g. class ID range for assistance. */
        final String classesRange;
        ChatStatus chatStatus;
        int unreadCount;

        JobPosting(int id, String courseId, String courseTitle, String moName,
                   LocalDateTime postTime, String description, String jobRequirements,
                   String taughtBy, String classesRange,
                   ChatStatus chatStatus, int unreadCount) {
            this.id = id;
            this.courseId = courseId;
            this.courseTitle = courseTitle;
            this.moName = moName;
            this.postTime = postTime;
            this.description = description;
            this.jobRequirements = jobRequirements;
            this.taughtBy = taughtBy;
            this.classesRange = classesRange;
            this.chatStatus = chatStatus;
            this.unreadCount = unreadCount;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            find_job f = new find_job();
            f.setVisible(true);
        });
    }
}
