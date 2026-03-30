package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class JobDetailPage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#fff5f8")),
                new Stop(1, Color.web("#fff9e6"))
        };
        LinearGradient pageBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops
        );
        root.setBackground(new Background(new BackgroundFill(pageBg, CornerRadii.EMPTY, Insets.EMPTY)));

        HBox topBar = createNavBar("BUPT-TA", "Job Detail page");
        VBox sideBar = createSideBar(stage);
        AnchorPane mainContent = createMainContent();
        Region bottomBar = createBottomBar();

        root.setTop(topBar);
        root.setLeft(sideBar);
        root.setCenter(mainContent);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Job Detail Page");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(String leftText, String rightText) {
        HBox navBar = new HBox();
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(12, 24, 12, 24));

        Stop[] navStops = new Stop[]{
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
        };
        LinearGradient navGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE, navStops
        );
        navBar.setBackground(new Background(new BackgroundFill(navGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label leftLabel = new Label(leftText);
        leftLabel.setTextFill(Color.BLACK);
        leftLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rightLabel = new Label(rightText);
        rightLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        rightLabel.setTextFill(Color.web("#1f3b8f"));

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

    private VBox createSideBar(Stage stage) {
        VBox sideBar = new VBox();
        sideBar.setPrefWidth(180);
        sideBar.setBackground(new Background(
                new BackgroundFill(Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Button dashBtn = createMenuButton("Dash Board", false);
        Button jobsBtn = createMenuButton("More Jobs", true);
        Button resumeBtn = createMenuButton("Resume\nDatabase", false);
        Button statusBtn = createMenuButton("application\nstatus", false);

        dashBtn.setOnAction(e -> System.out.println("Go to dashboard"));
        jobsBtn.setOnAction(e -> System.out.println("Already on more jobs"));
        resumeBtn.setOnAction(e -> System.out.println("Go to resume database"));
        statusBtn.setOnAction(e -> System.out.println("Go to application status"));

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, jobsBtn, resumeBtn, statusBtn, filler);
        return sideBar;
    }

    private Button createMenuButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setPrefWidth(180);
        button.setMinHeight(70);
        button.setWrapText(true);
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

    private AnchorPane createMainContent() {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        StackPane courseTag = createTag("Course Title :   EBU6501-Middleware", 500);
        StackPane teacherTag = createTag("Taught By :   Eric", 280);
        StackPane classTag = createTag("Classes in Need of Assistance :   2024215117 ~ 2024215120", 860);

        VBox descriptionBox = createInfoBox("Job Description :  Assist 5 labs & 3 tests.");
        VBox requirementBox = createInfoBox(
                "Job Requirements :  You are required to complete the summary of all tasks on time,\n" +
                "ensuring the accuracy and proper formatting of the data, and synchronizing the\n" +
                "progress in a timely manner."
        );

        Button backButton = createBackButton();

        AnchorPane.setTopAnchor(courseTag, 70.0);
        AnchorPane.setLeftAnchor(courseTag, 40.0);

        AnchorPane.setTopAnchor(teacherTag, 70.0);
        AnchorPane.setRightAnchor(teacherTag, 40.0);

        AnchorPane.setTopAnchor(classTag, 155.0);
        AnchorPane.setLeftAnchor(classTag, 40.0);

        AnchorPane.setTopAnchor(descriptionBox, 250.0);
        AnchorPane.setLeftAnchor(descriptionBox, 40.0);
        AnchorPane.setRightAnchor(descriptionBox, 40.0);

        AnchorPane.setTopAnchor(requirementBox, 430.0);
        AnchorPane.setLeftAnchor(requirementBox, 40.0);
        AnchorPane.setRightAnchor(requirementBox, 40.0);

        AnchorPane.setBottomAnchor(backButton, 30.0);
        AnchorPane.setLeftAnchor(backButton, 40.0);

        main.getChildren().addAll(
                courseTag,
                teacherTag,
                classTag,
                descriptionBox,
                requirementBox,
                backButton
        );

        return main;
    }

    private StackPane createTag(String text, double width) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        StackPane tag = new StackPane(label);
        tag.setAlignment(Pos.CENTER_LEFT);
        tag.setPadding(new Insets(0, 25, 0, 25));
        tag.setPrefWidth(width);
        tag.setPrefHeight(58);

        tag.setBackground(new Background(
                new BackgroundFill(Color.web("#ffb3d9"), new CornerRadii(30), Insets.EMPTY)
        ));

        return tag;
    }

    private VBox createInfoBox(String text) {
        VBox box = new VBox();
        box.setPadding(new Insets(28));
        box.setSpacing(10);

        box.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(2)
        )));

        Label content = new Label(text);
        content.setWrapText(true);
        content.setTextFill(Color.web("#333333"));
        content.setFont(Font.font("Arial", 18));

        box.getChildren().add(content);
        return box;
    }

    private Button createBackButton() {
        Button button = new Button("⬅");
        button.setPrefSize(56, 56);
        button.setStyle(
                "-fx-background-color: #b266ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 28;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    private Region createBottomBar() {
        Region bottomBar = new Region();
        bottomBar.setPrefHeight(18);

        Stop[] bottomStops = new Stop[]{
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
        };
        LinearGradient bottomGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE, bottomStops
        );

        bottomBar.setBackground(new Background(
                new BackgroundFill(bottomGradient, CornerRadii.EMPTY, Insets.EMPTY)
        ));
        return bottomBar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}