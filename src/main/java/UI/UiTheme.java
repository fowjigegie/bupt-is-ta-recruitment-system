package UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

final class UiTheme {
    static final double WINDOW_WIDTH = 1350;
    static final double WINDOW_HEIGHT = 820;

    private record NavEntry(String text, PageId pageId) {
    }

    private UiTheme() {
    }

    static Scene createScene(Parent root) {
        return new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    static BorderPane createPage(String pageTitle, Node sideBar, Node center, NavigationManager nav, UiAppContext context) {
        BorderPane root = new BorderPane();
        root.setBackground(pageBackground());
        root.setTop(createTopBar(pageTitle, nav, context));
        root.setCenter(center);
        if (sideBar != null) {
            root.setLeft(sideBar);
        }
        root.setBottom(createBottomBar());
        return root;
    }

    static Background pageBackground() {
        return new Background(new BackgroundFill(
            new LinearGradient(
                0,
                0,
                1,
                1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff5f8")),
                new Stop(1, Color.web("#fff9e6"))
            ),
            CornerRadii.EMPTY,
            Insets.EMPTY
        ));
    }

    static HBox createTopBar(String pageTitle, NavigationManager nav, UiAppContext context) {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setBackground(new Background(new BackgroundFill(
            new LinearGradient(
                0,
                0,
                1,
                0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
            ),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));

        Label leftLabel = new Label("BUPT-TA");
        leftLabel.setTextFill(Color.BLACK);
        leftLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label titleLabel = new Label(pageTitle);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#1f3b8f"));

        HBox rightGroup = new HBox(10);
        rightGroup.setAlignment(Pos.CENTER);
        rightGroup.getChildren().add(titleLabel);

        if (context.session().isAuthenticated()) {
            Label userLabel = new Label(context.session().displayName() + " | " + context.session().role().name());
            userLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            userLabel.setTextFill(Color.web("#4c5f99"));

            Button logoutButton = createSoftButton("Logout", 92, 34);
            logoutButton.setOnAction(event -> nav.logout());
            rightGroup.getChildren().addAll(userLabel, logoutButton);
        }

        Polygon redIcon = new Polygon(
            0.0, 8.0,
            8.0, 0.0,
            16.0, 8.0,
            8.0, 16.0
        );
        redIcon.setFill(Color.web("#ff4d4d"));

        Circle purpleCircle = new Circle(9);
        purpleCircle.setFill(Color.web("#b266ff"));

        rightGroup.getChildren().addAll(redIcon, purpleCircle);
        bar.getChildren().addAll(leftLabel, spacer, rightGroup);
        return bar;
    }

    static HBox createLandingNavBar(String leftText, String rightText, Runnable clickAction) {
        HBox navBar = new HBox();
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(12, 24, 12, 24));
        navBar.setBackground(new Background(new BackgroundFill(
            new LinearGradient(
                0,
                0,
                1,
                0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
            ),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));

        Label leftLabel = new Label(leftText);
        leftLabel.setTextFill(Color.BLACK);
        leftLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rightLabel = new Label(rightText);
        rightLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        rightLabel.setTextFill(Color.web("#1f3b8f"));

        if (clickAction != null) {
            rightLabel.setUnderline(true);
            rightLabel.setStyle("-fx-cursor: hand;");
            rightLabel.setOnMouseClicked(event -> clickAction.run());
        }

        Polygon redIcon = new Polygon(
            0.0, 8.0,
            8.0, 0.0,
            16.0, 8.0,
            8.0, 16.0
        );
        redIcon.setFill(Color.web("#ff4d4d"));

        Circle purpleCircle = new Circle(9);
        purpleCircle.setFill(Color.web("#b266ff"));

        HBox rightGroup = new HBox(10, rightLabel, redIcon, purpleCircle);
        rightGroup.setAlignment(Pos.CENTER);
        navBar.getChildren().addAll(leftLabel, spacer, rightGroup);
        return navBar;
    }

    static StackPane createIllustrationCard(String titleText, String subtitleText) {
        StackPane leftPane = new StackPane();
        leftPane.setStyle(
            "-fx-background-color: white;" +
                "-fx-background-radius: 24;" +
                "-fx-border-radius: 24;" +
                "-fx-border-color: #f4d9e6;" +
                "-fx-border-width: 1;"
        );
        leftPane.setPadding(new Insets(20));

        VBox placeholder = new VBox(18);
        placeholder.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#8c5a6b"));

        Label subtitle = new Label(subtitleText);
        subtitle.setFont(Font.font("Arial", 16));
        subtitle.setTextFill(Color.web("#999999"));
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        Rectangle block1 = new Rectangle(280, 120);
        block1.setArcWidth(25);
        block1.setArcHeight(25);
        block1.setFill(Color.web("#ffe6f0"));

        Rectangle block2 = new Rectangle(220, 90);
        block2.setArcWidth(25);
        block2.setArcHeight(25);
        block2.setFill(Color.web("#fff2cc"));

        Rectangle block3 = new Rectangle(160, 70);
        block3.setArcWidth(25);
        block3.setArcHeight(25);
        block3.setFill(Color.web("#e6f0ff"));

        VBox graphics = new VBox(15, block1, block2, block3);
        graphics.setAlignment(Pos.CENTER);

        placeholder.getChildren().addAll(title, subtitle, graphics);
        leftPane.getChildren().add(placeholder);
        return leftPane;
    }

    static VBox createFormCard(Node... children) {
        VBox container = new VBox(18);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(30));
        container.setMaxWidth(Double.MAX_VALUE);
        container.setBackground(new Background(new BackgroundFill(
            new LinearGradient(
                0,
                0,
                1,
                1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffd6e8")),
                new Stop(1, Color.web("#ffcce6"))
            ),
            new CornerRadii(20),
            Insets.EMPTY
        )));
        container.getChildren().addAll(children);
        return container;
    }

    static VBox createApplicantSidebar(NavigationManager nav, PageId selectedPage) {
        return createSidebar(
            nav,
            selectedPage,
            List.of(
                new NavEntry("Dash Board", PageId.APPLICANT_DASHBOARD),
                new NavEntry("More Jobs", PageId.MORE_JOBS),
                new NavEntry("Resume\nDatabase", PageId.RESUME_DATABASE),
                new NavEntry("application\nstatus", PageId.INTERVIEW_INVITATION)
            )
        );
    }

    static VBox createMoSidebar(NavigationManager nav, PageId selectedPage) {
        return createSidebar(
            nav,
            selectedPage,
            List.of(
                new NavEntry("Dash Board", PageId.MO_DASHBOARD),
                new NavEntry("Post\nVacancies", PageId.POST_VACANCIES),
                new NavEntry("Job\nManagement", PageId.JOB_MANAGEMENT),
                new NavEntry("Application\nreview", PageId.APPLICATION_REVIEW)
            )
        );
    }

    private static VBox createSidebar(NavigationManager nav, PageId selectedPage, List<NavEntry> entries) {
        VBox sideBar = new VBox();
        sideBar.setPrefWidth(190);
        sideBar.setBackground(new Background(new BackgroundFill(
            Color.web("#f5b4e6"),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));

        for (NavEntry entry : entries) {
            boolean selected = entry.pageId() == selectedPage;
            Button button = createSidebarButton(entry.text(), selected);
            button.setOnAction(event -> {
                // MO: Post Vacancies should default to \"new posting\" unless explicitly triggered by Edit Details.
                if (entry.pageId() == PageId.POST_VACANCIES) {
                    nav.context().clearJobEdit();
                }
                // MO: Sidebar entry should show ALL applications by default (not filtered by a previously selected job).
                if (entry.pageId() == PageId.APPLICATION_REVIEW) {
                    nav.context().selectJob(null);
                    nav.context().selectApplication(null);
                }
                nav.goTo(entry.pageId());
            });
            sideBar.getChildren().add(button);
        }

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);
        sideBar.getChildren().add(filler);
        return sideBar;
    }

    static Button createSidebarButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setPrefWidth(190);
        button.setMinHeight(76);
        button.setWrapText(true);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        if (selected) {
            button.setStyle(
                "-fx-background-color: #4565a8;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 0 35 35 0;" +
                    "-fx-cursor: hand;"
            );
        } else {
            button.setStyle(
                "-fx-background-color: #f5b4e6;" +
                    "-fx-text-fill: white;" +
                    "-fx-border-color: white;" +
                    "-fx-border-width: 0 0 2 0;" +
                    "-fx-background-radius: 0;" +
                    "-fx-cursor: hand;"
            );
        }

        return button;
    }

    static Button createPrimaryButton(String text, double width, double height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setWrapText(true);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.ITALIC, 22));
        button.setStyle(
            "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                "-fx-text-fill: #ff66b3;" +
                "-fx-background-radius: 28;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    static Button createSoftButton(String text, double width, double height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        button.setStyle(
            "-fx-background-color: #ffd6e8;" +
                "-fx-text-fill: #333333;" +
                "-fx-background-radius: 22;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    static Button createOutlineButton(String text, double width, double height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        button.setStyle(
            "-fx-background-color: white;" +
                "-fx-text-fill: #333333;" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: #f0a6e9;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 22;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    static Button createBackButton(NavigationManager nav) {
        Button button = new Button("<");
        button.setPrefSize(56, 56);
        button.setStyle(
            "-fx-background-color: #b266ff;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 28;" +
                "-fx-cursor: hand;"
        );
        button.setOnAction(event -> nav.goBack());
        return button;
    }

    static Label createPageHeading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 44));
        label.setTextFill(Color.web("#4664a8"));
        return label;
    }

    static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        label.setTextFill(Color.web("#4664a8"));
        return label;
    }

    static Label createMutedText(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 15));
        label.setTextFill(Color.web("#8b7fa0"));
        label.setWrapText(true);
        return label;
    }

    static StackPane createTag(String text, double width) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        label.setWrapText(true);

        StackPane tag = new StackPane(label);
        tag.setAlignment(Pos.CENTER_LEFT);
        tag.setPadding(new Insets(0, 24, 0, 24));
        tag.setPrefWidth(width);
        tag.setPrefHeight(58);
        tag.setBackground(new Background(new BackgroundFill(
            Color.web("#ffb3d9"),
            new CornerRadii(30),
            Insets.EMPTY
        )));
        return tag;
    }

    static VBox createWhiteCard(String title, String body) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(24));
        box.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        box.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(2)
        )));

        Label heading = createSectionTitle(title);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        Label content = new Label(body);
        content.setFont(Font.font("Arial", 17));
        content.setTextFill(Color.web("#333333"));
        content.setWrapText(true);

        box.getChildren().addAll(heading, content);
        return box;
    }

    static VBox createPlaceholderCard(String title, String body) {
        VBox box = createWhiteCard(title, body);
        box.setPrefWidth(320);
        return box;
    }

    static VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.setPrefSize(240, 150);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_LEFT);
        card.setBackground(new Background(new BackgroundFill(
            Color.WHITE,
            new CornerRadii(24),
            Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        Label titleLabel = createMutedText(title);
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        valueLabel.setTextFill(Color.web("#4664a8"));

        Label subtitleLabel = createMutedText(subtitle);
        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    static Hyperlink createInlineLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setFont(Font.font("Arial", 14));
        link.setTextFill(Color.web("#3366cc"));
        link.setBorder(Border.EMPTY);
        link.setPadding(Insets.EMPTY);
        return link;
    }

    static Region createBottomBar() {
        Region bottom = new Region();
        bottom.setPrefHeight(18);
        bottom.setBackground(new Background(new BackgroundFill(
            new LinearGradient(
                0,
                0,
                1,
                0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
            ),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));
        return bottom;
    }

    static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
