package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.admin.WorkloadSummary;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.assistant.NvidiaAiAssistantClient;
import com.bupt.tarecruitment.auth.UserRole;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Admin-only NVIDIA analysis entry points.
 */
final class AdminAiAnalysisDialog {
    private static final String SYSTEM_PROMPT = """
        You are an AI analyst for the BUPT-TA Recruitment System admin dashboard.
        Use only the system snapshot supplied by the application.
        Do not invent users, jobs, applications, CVs, schedules, skills, or message records.
        If a fact is missing, say that the system data is missing it.
        Use plain text only. Do not use Markdown syntax, tables, code fences, headings with #, or **bold**.
        Write compact admin-facing analysis with short labelled sections.
        End with a short practical recommendation section.
        """;

    private AdminAiAnalysisDialog() {
    }

    static void showSystemAnalysis(Window owner, UiAppContext context) {
        showAnalysisDialog(
            owner,
            "AI system analysis",
            "NVIDIA overview using users, jobs, applications, workload, and integrity checks.",
            () -> buildSystemPrompt(context),
            900
        );
    }

    static void showJobPostingReview(Window owner, UiAppContext context, JobPosting job) {
        showAnalysisDialog(
            owner,
            "AI posting review",
            "NVIDIA review for this job posting's quality and applicant appeal.",
            () -> buildJobReviewPrompt(context, job),
            700
        );
    }

    private static void showAnalysisDialog(
        Window owner,
        String titleText,
        String subtitleText,
        PromptSupplier promptSupplier,
        int maxTokens
    ) {
        Stage dialog = new Stage();
        dialog.setTitle(titleText);
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label title = new Label(titleText);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#4664a8"));

        Label providerTag = createProviderTag();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(14, title, spacer, providerTag);
        header.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#8b7fa0"));

        VBox transcript = new VBox(12);
        transcript.setPadding(new Insets(8, 6, 8, 6));
        transcript.getChildren().addAll(
            createAssistantBubble("I will analyze the current admin data and return a concise management review."),
            createMetaLabel("Assistant provider: " + createProviderText())
        );

        Label thinkingLabel = createBubbleLabel("Thinking...");
        transcript.getChildren().add(wrapAssistantBubble(thinkingLabel));

        ScrollPane resultScroll = new ScrollPane(transcript);
        resultScroll.setFitToWidth(true);
        resultScroll.setPrefViewportHeight(460);
        resultScroll.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:#ffffff;" +
                "-fx-border-color: transparent;"
        );

        Button closeButton = UiTheme.createOutlineButton("Close", 120, 42);
        closeButton.setOnAction(event -> dialog.close());
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox shell = new VBox(16, header, subtitle, resultScroll, footer);
        shell.setPadding(new Insets(24));
        shell.setBackground(UiTheme.pageBackground());

        Timeline thinkingAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, event -> thinkingLabel.setText("Thinking.")),
            new KeyFrame(Duration.millis(350), event -> thinkingLabel.setText("Thinking..")),
            new KeyFrame(Duration.millis(700), event -> thinkingLabel.setText("Thinking..."))
        );
        thinkingAnimation.setCycleCount(Timeline.INDEFINITE);
        thinkingAnimation.play();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                Optional<NvidiaAiAssistantClient> client = NvidiaAiAssistantClient.fromEnvironment();
                if (client.isEmpty() || !client.get().isConfigured()) {
                    return "Cloud AI is not configured for this Java process yet.\n\n"
                        + "You can still use the local dashboard tools. To enable Cloud AI analysis, set DASHSCOPE_API_KEY or NVIDIA_API_KEY before launching the app.";
                }
                String answer = client.get().chat(SYSTEM_PROMPT, promptSupplier.get(), maxTokens);
                if (answer == null || answer.isBlank()) {
                    return "Cloud AI returned an empty answer for this admin analysis.\n\n"
                        + "The API was reachable, but no readable chat content was returned. Please try again, or ask a shorter admin analysis question.";
                }
                return answer;
            }
        };
        task.setOnSucceeded(event -> {
            thinkingAnimation.stop();
            thinkingLabel.setText(task.getValue());
            resultScroll.setVvalue(1.0);
        });
        task.setOnFailed(event -> {
            thinkingAnimation.stop();
            thinkingLabel.setText(
                "Cloud AI analysis could not be completed right now.\n\n"
                    + summarizeError(task.getException())
            );
            resultScroll.setVvalue(1.0);
        });
        Thread worker = new Thread(task, "admin-ai-analysis");
        worker.setDaemon(true);
        worker.start();

        dialog.setScene(new Scene(shell, 900, 660));
        dialog.showAndWait();
    }

    private static Label createProviderTag() {
        Label tag = new Label(createProviderText());
        tag.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        tag.setTextFill(Color.web("#b05a88"));
        tag.setStyle(
            "-fx-background-color: #ffe9f3;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #f3b7db;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 16;" +
                "-fx-padding: 6 12 6 12;"
        );
        return tag;
    }

    private static String createProviderText() {
        Optional<NvidiaAiAssistantClient> client = NvidiaAiAssistantClient.fromEnvironment();
        return client.map(value -> "Cloud AI: " + value.model()).orElse("Built-in tools only");
    }

    private static Node createAssistantBubble(String text) {
        Label bubble = createBubbleLabel(text);
        return wrapAssistantBubble(bubble);
    }

    private static HBox wrapAssistantBubble(Label bubble) {
        applyAssistantBubbleStyle(bubble);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label createBubbleLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(650);
        return label;
    }

    private static void applyAssistantBubbleStyle(Label bubble) {
        bubble.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 18 18 18 6;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 18 18 18 6;" +
                "-fx-text-fill: #5c3f6b;" +
                "-fx-font-size: 15px;" +
                "-fx-padding: 12 14 12 14;"
        );
    }

    private static Label createMetaLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#8b7fa0"));
        label.setPadding(new Insets(2, 6, 4, 6));
        return label;
    }

    private static String buildSystemPrompt(UiAppContext context) {
        List<JobApplication> applications = context.services().applicationRepository().findAll();
        List<JobPosting> jobs = context.services().jobRepository().findAll();
        List<WorkloadSummary> workloads = context.services().adminWorkloadService().listAcceptedTaWorkloads(10);
        AdminDataIntegrityPanel.IntegrityReport integrityReport = AdminDataIntegrityPanel.analyze(context);

        Map<String, Long> applicationsByJob = applications.stream()
            .collect(Collectors.groupingBy(JobApplication::jobId, Collectors.counting()));
        List<JobPosting> topJobs = jobs.stream()
            .sorted(Comparator
                .comparingLong((JobPosting job) -> applicationsByJob.getOrDefault(job.jobId(), 0L))
                .reversed()
                .thenComparing(JobPosting::jobId))
            .limit(6)
            .toList();
        List<JobPosting> lowApplicationOpenJobs = jobs.stream()
            .filter(job -> job.status() == JobStatus.OPEN)
            .filter(job -> applicationsByJob.getOrDefault(job.jobId(), 0L) == 0)
            .sorted(Comparator.comparing(JobPosting::jobId))
            .limit(8)
            .toList();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Task: Analyze the current admin dashboard and answer these points: system health, recruitment flow blockers, jobs with too many or too few applications, TA workload risks, and next admin actions.\n\n");
        prompt.append("System snapshot:\n");
        prompt.append("Users excluding admin: ").append(countUsers(context, null)).append('\n');
        prompt.append("Applicants: ").append(countUsers(context, UserRole.APPLICANT)).append('\n');
        prompt.append("Module organisers: ").append(countUsers(context, UserRole.MO)).append('\n');
        prompt.append("Jobs: ").append(jobs.size()).append(", open: ")
            .append(jobs.stream().filter(job -> job.status() == JobStatus.OPEN).count())
            .append(", closed: ")
            .append(jobs.stream().filter(job -> job.status() == JobStatus.CLOSED).count())
            .append('\n');
        prompt.append("Applications: ").append(applications.size()).append('\n');
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = applications.stream().filter(application -> application.status() == status).count();
            prompt.append("Applications ").append(status.name()).append(": ").append(count).append('\n');
        }
        prompt.append("Data integrity: ")
            .append(integrityReport.criticalCount()).append(" critical issues, ")
            .append(integrityReport.warningCount()).append(" warnings.\n");
        prompt.append("Workload summaries:\n");
        if (workloads.isEmpty()) {
            prompt.append("No accepted TA workload records.\n");
        } else {
            for (WorkloadSummary summary : workloads) {
                prompt.append("- ")
                    .append(summary.applicantDisplayName()).append(" (").append(summary.applicantUserId()).append("): ")
                    .append(summary.acceptedAssignments().size()).append(" accepted jobs, ")
                    .append(summary.totalWeeklyHours()).append(" weekly hours, conflicts ")
                    .append(summary.conflicts().size()).append(", invalid slots ")
                    .append(summary.invalidScheduleEntries().size()).append(", overloaded ")
                    .append(summary.overloaded()).append('\n');
            }
        }
        prompt.append("Top jobs by applications:\n");
        for (JobPosting job : topJobs) {
            prompt.append("- ")
                .append(job.title()).append(" (").append(job.jobId()).append("): ")
                .append(applicationsByJob.getOrDefault(job.jobId(), 0L)).append(" applications, status ")
                .append(job.status()).append('\n');
        }
        prompt.append("Open jobs with zero applications:\n");
        if (lowApplicationOpenJobs.isEmpty()) {
            prompt.append("None.\n");
        } else {
            for (JobPosting job : lowApplicationOpenJobs) {
                prompt.append("- ")
                    .append(job.title()).append(" (").append(job.jobId()).append("), skills ")
                    .append(String.join(", ", job.requiredSkills())).append(", schedule slots ")
                    .append(job.scheduleSlots().size()).append('\n');
            }
        }
        prompt.append("\nWrite the answer in plain text with these sections: Health, Flow blockers, Application distribution, Workload risks, Next actions.");
        return prompt.toString();
    }

    private static String buildJobReviewPrompt(UiAppContext context, JobPosting job) {
        List<JobApplication> applications = context.services().applicationRepository().findAll().stream()
            .filter(application -> application.jobId().equals(job.jobId()))
            .toList();
        String organiserName = context.displayNameForUser(job.organiserId());
        String placeholderCheck = looksPlaceholderTitle(job.title()) ? "Title looks placeholder-like." : "Title looks descriptive enough.";
        String descriptionCheck = job.description().trim().length() < 50 ? "Description is short." : "Description has useful length.";
        String skillsCheck = job.requiredSkills().isEmpty() ? "Required skills are missing." : "Required skills: " + String.join(", ", job.requiredSkills());
        String scheduleCheck = job.scheduleSlots().isEmpty() ? "Schedule slots are missing." : "Schedule slots: " + String.join("; ", job.scheduleSlots());

        StringBuilder prompt = new StringBuilder();
        prompt.append("Task: Review this job posting for admin quality control.\n");
        prompt.append("Check whether the description is too short, required skills are clear, schedule is missing, title looks like a placeholder, and whether it may attract too few applications.\n\n");
        prompt.append("Job data:\n");
        prompt.append("Job id: ").append(job.jobId()).append('\n');
        prompt.append("Title: ").append(job.title()).append('\n');
        prompt.append("Organiser: ").append(organiserName).append(" (").append(job.organiserId()).append(")\n");
        prompt.append("Module/activity: ").append(job.moduleOrActivity()).append('\n');
        prompt.append("Activity type: ").append(job.activityType()).append('\n');
        prompt.append("Status: ").append(job.status()).append('\n');
        prompt.append("Weekly hours: ").append(job.weeklyHours()).append('\n');
        prompt.append("Description: ").append(job.description()).append('\n');
        prompt.append("Required skills: ").append(job.requiredSkills().isEmpty() ? "(none)" : String.join(", ", job.requiredSkills())).append('\n');
        prompt.append("Schedule slots: ").append(job.scheduleSlots().isEmpty() ? "(none)" : String.join("; ", job.scheduleSlots())).append('\n');
        prompt.append("Application count: ").append(applications.size()).append('\n');
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = applications.stream().filter(application -> application.status() == status).count();
            prompt.append("Applications ").append(status.name()).append(": ").append(count).append('\n');
        }
        prompt.append("Local pre-checks:\n");
        prompt.append("- ").append(placeholderCheck).append('\n');
        prompt.append("- ").append(descriptionCheck).append('\n');
        prompt.append("- ").append(skillsCheck).append('\n');
        prompt.append("- ").append(scheduleCheck).append('\n');
        prompt.append("\nWrite the answer in plain text with these sections: Overall verdict, Data quality, Applicant appeal, Admin action.");
        return prompt.toString();
    }

    private static long countUsers(UiAppContext context, UserRole role) {
        return context.services().userRepository().findAll().stream()
            .filter(user -> user.role() != UserRole.ADMIN)
            .filter(user -> role == null || user.role() == role)
            .count();
    }

    private static boolean looksPlaceholderTitle(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase();
        return normalized.isBlank()
            || normalized.matches("\\d+")
            || normalized.equals("test")
            || normalized.equals("demo")
            || normalized.contains("placeholder")
            || normalized.startsWith("job ");
    }

    private static String summarizeError(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error.";
        }
        if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    @FunctionalInterface
    private interface PromptSupplier {
        String get();
    }
}
