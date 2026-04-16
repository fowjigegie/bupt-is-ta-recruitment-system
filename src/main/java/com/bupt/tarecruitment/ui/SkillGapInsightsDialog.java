package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.recommendation.SkillGapAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
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

import java.util.List;
import java.util.Optional;

/**
 * 用弹窗展示 US10 的详细技能差距分析，不打乱当前 Job Detail 主布局。
 */
public final class SkillGapInsightsDialog {
    private SkillGapInsightsDialog() {
    }

    public static void show(Window owner, UiAppContext context, JobPosting job) {
        if (!context.session().isAuthenticated()) {
            UiTheme.showInfo("Skill gap insights", "Please log in as an applicant to view detailed analysis.");
            return;
        }

        Optional<SkillGapAnalysis> analysis = context.services().skillGapAnalysisService()
            .analysisForApplicantAndJob(context.session().userId(), job.jobId());

        if (analysis.isEmpty()) {
            UiTheme.showInfo(
                "Skill gap insights",
                "Create or update your profile in Resume Database before opening detailed skill analysis."
            );
            return;
        }

        Stage dialog = new Stage();
        dialog.setTitle("Skill Gap Insights");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ScrollPane scrollPane = new ScrollPane(buildContent(dialog, job, analysis.get()));
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background:#ffffff;" +
                "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;"
        );

        Scene scene = new Scene(scrollPane, 920, 700);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static VBox buildContent(Stage dialog, JobPosting job, SkillGapAnalysis analysis) {
        VBox content = new VBox(18);
        content.setPadding(new Insets(26, 30, 26, 30));
        content.setBackground(UiTheme.pageBackground());

        Label heading = UiTheme.createPageHeading("Skill Gap Insights");
        heading.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 34px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        Label jobLabel = UiTheme.createMutedText("For " + job.title() + " | " + job.moduleOrActivity());
        Label headlineLabel = new Label(analysis.readinessHeadline());
        headlineLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        headlineLabel.setTextFill(Color.web("#b05a88"));

        VBox summaryCard = createCard(
            "Readiness summary",
            createReadinessBar(analysis),
            createLegendRow(analysis),
            createBodyLabel(analysis.summary())
        );

        HBox skillColumns = new HBox(16,
            createCard("Matched", createSkillList(analysis.feedback().matchedSkills(), "#2e7d32", "#e9f7ef")),
            createCard("Weakly matched", createSkillList(analysis.feedback().weaklyMatchedSkills(), "#c77800", "#fff4df")),
            createCard("Missing", createSkillList(analysis.feedback().missingSkills(), "#b00020", "#fdeced"))
        );
        HBox.setHgrow(skillColumns.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(skillColumns.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(skillColumns.getChildren().get(2), Priority.ALWAYS);

        VBox weakBridgeCard = createCard(
            "Why these weak matches count",
            createWeakMatchList(analysis.weakMatchExplanations())
        );

        VBox nextSkillsCard = createCard(
            "Best next skills to improve",
            createPrioritySuggestions(analysis.prioritySkillSuggestions())
        );

        VBox whatIfCard = createCard(
            "What-if simulation",
            createImprovementScenarios(analysis.improvementScenarios())
        );

        var closeButton = UiTheme.createOutlineButton("Close", 160, 46);
        closeButton.setOnAction(event -> dialog.close());
        HBox actions = new HBox(closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(
            heading,
            jobLabel,
            headlineLabel,
            summaryCard,
            skillColumns,
            weakBridgeCard,
            nextSkillsCard,
            whatIfCard,
            actions
        );
        return content;
    }

    private static HBox createReadinessBar(SkillGapAnalysis analysis) {
        int total = Math.max(analysis.feedback().totalRequiredSkillCount(), 1);
        HBox bar = new HBox();
        bar.setPrefHeight(24);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setBorder(new Border(new BorderStroke(
            Color.web("#ead8e4"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.5)
        )));

        Region matched = createBarSegment(analysis.feedback().matchedRequiredSkillCount(), total, "#2e7d32", true, false);
        Region weak = createBarSegment(analysis.feedback().weaklyMatchedRequiredSkillCount(), total, "#f0a236", false, false);
        Region missing = createBarSegment(analysis.feedback().missingSkills().size(), total, "#d94b68", false, true);

        if (analysis.feedback().matchedRequiredSkillCount() == 0
            && analysis.feedback().weaklyMatchedRequiredSkillCount() == 0
            && analysis.feedback().missingSkills().isEmpty()) {
            matched.setStyle(
                "-fx-background-color: #d7ecff;" +
                    "-fx-background-radius: 18;"
            );
        }

        bar.getChildren().addAll(matched, weak, missing);
        return bar;
    }

    private static Region createBarSegment(int count, int total, String color, boolean roundLeft, boolean roundRight) {
        Region region = new Region();
        double percentage = count == 0 ? 0 : count / (double) total;
        region.setPrefWidth(Math.max(percentage * 560, count == 0 ? 0 : 36));
        HBox.setHgrow(region, Priority.ALWAYS);

        String radius;
        if (roundLeft && roundRight) {
            radius = "18";
        } else if (roundLeft) {
            radius = "18 0 0 18";
        } else if (roundRight) {
            radius = "0 18 18 0";
        } else {
            radius = "0";
        }

        region.setStyle(
            "-fx-background-color: " + color + ";" +
                "-fx-background-radius: " + radius + ";"
        );
        return region;
    }

    private static HBox createLegendRow(SkillGapAnalysis analysis) {
        HBox legend = new HBox(14);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
            createLegendItem("#2e7d32", "Matched: " + analysis.feedback().matchedRequiredSkillCount()),
            createLegendItem("#f0a236", "Weak: " + analysis.feedback().weaklyMatchedRequiredSkillCount()),
            createLegendItem("#d94b68", "Missing: " + analysis.feedback().missingSkills().size()),
            createLegendItem("#4664a8", "Coverage: " + analysis.feedback().coveragePercent() + "%")
        );
        return legend;
    }

    private static HBox createLegendItem(String color, String text) {
        Region dot = new Region();
        dot.setPrefSize(12, 12);
        dot.setStyle(
            "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 6;"
        );

        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#5c3f6b"));

        HBox item = new HBox(8, dot, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private static FlowPane createSkillList(List<String> skills, String borderColor, String fillColor) {
        FlowPane flow = new FlowPane();
        flow.setHgap(10);
        flow.setVgap(10);

        if (skills.isEmpty()) {
            flow.getChildren().add(createBodyLabel("(none)"));
            return flow;
        }

        for (String skill : skills) {
            flow.getChildren().add(createSkillChip(skill, borderColor, fillColor));
        }
        return flow;
    }

    private static VBox createWeakMatchList(List<SkillGapAnalysis.WeakMatchExplanation> explanations) {
        VBox box = new VBox(12);
        if (explanations.isEmpty()) {
            box.getChildren().add(createBodyLabel("No weak matches were detected for this job."));
            return box;
        }

        for (SkillGapAnalysis.WeakMatchExplanation explanation : explanations) {
            Label bridge = new Label(explanation.requiredSkill() + " <- supported by " + explanation.supportingSkill());
            bridge.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            bridge.setTextFill(Color.web("#b05a88"));

            Label body = createBodyLabel(explanation.explanation());
            VBox item = new VBox(4, bridge, body);
            item.setPadding(new Insets(10, 14, 10, 14));
            item.setBackground(new Background(new BackgroundFill(
                Color.web("#fff7fb"),
                new CornerRadii(18),
                Insets.EMPTY
            )));
            item.setBorder(new Border(new BorderStroke(
                Color.web("#f1c3da"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(18),
                new BorderWidths(1.3)
            )));
            box.getChildren().add(item);
        }
        return box;
    }

    private static VBox createPrioritySuggestions(List<SkillGapAnalysis.PrioritySkillSuggestion> suggestions) {
        VBox box = new VBox(12);
        if (suggestions.isEmpty()) {
            box.getChildren().add(createBodyLabel("You already cover all listed skills directly, so there is no urgent next skill to recommend."));
            return box;
        }

        int index = 1;
        for (SkillGapAnalysis.PrioritySkillSuggestion suggestion : suggestions) {
            Label title = new Label(index + ". " + suggestion.skill());
            title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
            title.setTextFill(Color.web("#4664a8"));

            Label body = createBodyLabel(suggestion.reason());
            box.getChildren().add(new VBox(4, title, body));
            index++;
        }
        return box;
    }

    private static VBox createImprovementScenarios(List<SkillGapAnalysis.ImprovementScenario> scenarios) {
        VBox box = new VBox(12);
        if (scenarios.isEmpty()) {
            box.getChildren().add(createBodyLabel("There are no remaining gap scenarios to simulate."));
            return box;
        }

        for (SkillGapAnalysis.ImprovementScenario scenario : scenarios) {
            Label title = new Label(scenario.title());
            title.setFont(Font.font("Arial", FontWeight.BOLD, 17));
            title.setTextFill(Color.web("#4664a8"));

            Label coverage = new Label("Projected readiness: " + scenario.projectedCoveragePercent() + "%");
            coverage.setFont(Font.font("Arial", FontWeight.BOLD, 15));
            coverage.setTextFill(Color.web("#2e7d32"));

            Label body = createBodyLabel(scenario.explanation());
            VBox item = new VBox(4, title, coverage, body);

            if (!scenario.newlyMatchedSkills().isEmpty()) {
                item.getChildren().add(createSkillList(
                    scenario.newlyMatchedSkills(),
                    "#2e7d32",
                    "#e9f7ef"
                ));
            }

            item.setPadding(new Insets(10, 14, 10, 14));
            item.setBackground(new Background(new BackgroundFill(
                Color.web("#f8fbff"),
                new CornerRadii(18),
                Insets.EMPTY
            )));
            item.setBorder(new Border(new BorderStroke(
                Color.web("#d6e5ff"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(18),
                new BorderWidths(1.3)
            )));
            box.getChildren().add(item);
        }
        return box;
    }

    private static VBox createCard(String title, javafx.scene.Node... children) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.8)
        )));

        Label heading = UiTheme.createSectionTitle(title);
        heading.setStyle(
            "-fx-font-family: Arial;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );
        card.getChildren().add(heading);
        card.getChildren().addAll(children);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Label createBodyLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", 15));
        label.setTextFill(Color.web("#5c3f6b"));
        return label;
    }

    private static Label createSkillChip(String text, String borderColor, String fillColor) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#5c3f6b"));
        label.setPadding(new Insets(8, 14, 8, 14));
        label.setStyle(
            "-fx-background-color: " + fillColor + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 18;" +
                "-fx-background-radius: 18;" +
                "-fx-text-fill: #5c3f6b;"
        );
        return label;
    }
}
