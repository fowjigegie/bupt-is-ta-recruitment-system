package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.common.skill.SkillCatalog;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

/**
 * 提供单独的技能选择页面，避免 Resume 页面过度拥挤。
 */
public class SkillSelectionPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.SKILL_SELECTOR, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        ResumeDraftState draftState = context.resumeDraft() != null
            ? context.resumeDraft()
            : fallbackDraft(context);

        SkillPicker skillPicker = SkillPicker.create(
            "Profile Skills",
            "Search across all categories, expand any category you want, or type a custom skill and add it to the current selection.",
            resolveSkillSuggestions(context)
        );
        skillPicker.setSkills(draftState.skills());

        var saveButton = UiTheme.createPrimaryButton("Use selected skills", 240, 54);
        saveButton.setOnAction(event -> {
            context.saveResumeDraft(draftState.withSkills(skillPicker.skills()));
            nav.goBack();
        });

        HBox actions = new HBox(16, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(
            18,
            UiTheme.createPageHeading("Skill Selection"),
            UiTheme.createWhiteCard(
                "How this works",
                "These selected skills are part of your profile and are used for job matching. "
                    + "They are now managed separately from individual CV text. "
                    + "After returning to the Resume page, you still need to create or update your profile to save them."
            ),
            skillPicker.container(),
            actions
        );
        content.setPadding(new Insets(28, 40, 28, 40));

        BorderPane root = UiTheme.createPage(
            "Skill Selection",
            UiTheme.createApplicantSidebar(nav, PageId.RESUME_DATABASE),
            content,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static ResumeDraftState fallbackDraft(UiAppContext context) {
        return context.services().profileRepository().findByUserId(context.session().userId())
            .map(profile -> new ResumeDraftState(
                null,
                profile.fullName(),
                fallbackGrade(profile.yearOfStudy(), profile.educationLevel()),
                profile.programme(),
                profile.studentId(),
                profile.availabilitySlots(),
                "",
                "",
                profile.skills(),
                String.join(System.lineSeparator(), profile.desiredPositions())
            ))
            .orElseGet(() -> new ResumeDraftState(
                null,
                "",
                "Senior undergraduate",
                "",
                "",
                List.of(),
                "",
                "",
                List.of(),
                ""
            ));
    }

    private static String fallbackGrade(int year, String educationLevel) {
        boolean graduated = "graduated".equalsIgnoreCase(educationLevel);
        if (!graduated && year == 4) {
            return "Senior undergraduate";
        }
        return switch (year) {
            case 1 -> "First-year graduate";
            case 2 -> "Second-year graduate";
            case 3 -> "Third-year graduate";
            default -> "Senior undergraduate";
        };
    }

    private static Map<String, List<String>> resolveSkillSuggestions(UiAppContext context) {
        List<String> jobSkills = context.services().jobRepository().findAll().stream()
            .flatMap(job -> job.requiredSkills().stream())
            .toList();
        return SkillCatalog.mergeSuggestedSkillCategories(jobSkills);
    }
}
