package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ModuleOrganizerDashboardPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MO_DASHBOARD, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        long ownedJobs = context.services().jobRepository().findAll().stream()
            .filter(job -> job.organiserId().equals(context.session().userId()))
            .count();
        long ownedApplications = context.services().applicationRepository().findAll().stream()
            .filter(application -> context.services().jobRepository().findByJobId(application.jobId())
                .map(job -> job.organiserId().equals(context.session().userId()))
                .orElse(false))
            .count();
        long pendingReviews = context.services().applicationRepository().findAll().stream()
            .filter(application -> context.services().jobRepository().findByJobId(application.jobId())
                .map(job -> job.organiserId().equals(context.session().userId()))
                .orElse(false))
            .filter(application -> application.status() == com.bupt.tarecruitment.application.ApplicationStatus.SUBMITTED)
            .count();

        VBox center = new VBox(24);
        center.setPadding(new Insets(30, 40, 30, 40));

        String displayName = context.session().displayName();
        center.getChildren().addAll(
            UiTheme.createPageHeading("Welcome back, " + displayName),
            UiTheme.createMutedText("Here is a clean MO shell for posting jobs, managing listings, and reviewing applicants.")
        );

        HBox stats = new HBox(20,
            UiTheme.createStatCard("Owned postings", Long.toString(ownedJobs), "These are the jobs currently stored for this organiser."),
            UiTheme.createStatCard("Applications", Long.toString(ownedApplications), "All applications tied to your jobs are now visible."),
            UiTheme.createStatCard("Pending reviews", Long.toString(pendingReviews), "Submitted applications can be opened from review and management pages.")
        );

        Button postButton = UiTheme.createPrimaryButton("Post vacancies", 240, 90);
        postButton.setOnAction(event -> nav.goTo(PageId.POST_VACANCIES));

        Button manageButton = UiTheme.createPrimaryButton("Job management", 240, 90);
        manageButton.setOnAction(event -> nav.goTo(PageId.JOB_MANAGEMENT));

        Button reviewButton = UiTheme.createPrimaryButton("Application review", 240, 90);
        reviewButton.setOnAction(event -> nav.goTo(PageId.APPLICATION_REVIEW));

        HBox quickActions = new HBox(24, postButton, manageButton, reviewButton);
        quickActions.setAlignment(Pos.CENTER_LEFT);

        HBox lowerCards = new HBox(20,
            UiTheme.createPlaceholderCard("Today's focus", "Confirm open positions, publish updated job requirements, and keep communication expectations clear for applicants."),
            UiTheme.createPlaceholderCard("Team note", "This page is intentionally static for now, but it already routes to the three main MO work areas."),
            UiTheme.createPlaceholderCard("Next step", "Once business hooks are ready, these cards can be wired into the job repository and review services.")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(stats, quickActions, lowerCards, spacer, footer);

        BorderPane root = UiTheme.createPage(
            "MO Dashboard",
            UiTheme.createMoSidebar(nav, PageId.MO_DASHBOARD),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
