package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class DashboardPages extends Application {

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

        HBox topBar = createNavBar("BUPT-TA", "Dashboard", false);
        VBox sideBar = createSideBar();
        VBox mainContent = createMainContent();

        root.setTop(topBar);
        root.setLeft(sideBar);
        root.setCenter(mainContent);

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(String leftText, String rightText, boolean clickableText) {
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

        if (clickableText) {
            rightLabel.setUnderline(true);
            rightLabel.setStyle("-fx-cursor: hand;");
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

    private VBox createSideBar() {
        VBox sideBar = new VBox();
        sideBar.setPrefWidth(180);
        sideBar.setBackground(new Background(
                new BackgroundFill(Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Button dashBtn = createMenuButton("Dash Board", true);
        Button jobsBtn = createMenuButton("More Jobs", false);
        Button resumeBtn = createMenuButton("Resume\nDatabase", false);
        Button statusBtn = createMenuButton("application\nstatus", false);

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

    private VBox createMainContent() {
        VBox container = new VBox(30);
        container.setPadding(new Insets(30, 40, 30, 40));

        HBox welcomeRow = createWelcomeRow();
        HBox featureButtons = createFeatureButtons();
        VBox jobsArea = createJobsArea();

        container.getChildren().addAll(welcomeRow, featureButtons, jobsArea);
        return container;
    }

    private HBox createWelcomeRow() {
        HBox welcomeRow = new HBox(18);
        welcomeRow.setAlignment(Pos.CENTER);

        Polygon leftDiamond = createBlueDiamond();
        Label welcomeLabel = new Label("Welcome Back");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        welcomeLabel.setTextFill(Color.web("#4664a8"));
        Polygon rightDiamond = createBlueDiamond();

        welcomeRow.getChildren().addAll(leftDiamond, welcomeLabel, rightDiamond);
        return welcomeRow;
    }

    private Polygon createBlueDiamond() {
        Polygon diamond = new Polygon(
                0.0, 12.0,
                12.0, 0.0,
                24.0, 12.0,
                12.0, 24.0
        );
        diamond.setFill(Color.web("#4664a8"));
        return diamond;
    }

    private HBox createFeatureButtons() {
        HBox row = new HBox(30);
        row.setAlignment(Pos.CENTER);

        Button invitationBtn = createMainActionButton("Interview\nInvitation");

        StackPane chatPane = new StackPane();
        Button chatBtn = createMainActionButton("Chat");

        Circle badgeCircle = new Circle(18);
        badgeCircle.setFill(Color.web("#ff3333"));

        Label badgeLabel = new Label("3");
        badgeLabel.setTextFill(Color.WHITE);
        badgeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        StackPane badge = new StackPane(badgeCircle, badgeLabel);
        badge.setTranslateX(75);
        badge.setTranslateY(-35);

        chatPane.getChildren().addAll(chatBtn, badge);

        row.getChildren().addAll(invitationBtn, chatPane);
        return row;
    }

    private Button createMainActionButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(220, 110);
        button.setWrapText(true);
        button.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 24));
        button.setTextFill(Color.WHITE);
        button.setStyle(
                "-fx-background-color: #f5a8bb;" +
                        "-fx-background-radius: 28;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    private VBox createJobsArea() {
        VBox jobsWrapper = new VBox(0);
        jobsWrapper.setSpacing(0);
        jobsWrapper.setMaxWidth(Double.MAX_VALUE);

        StackPane titleBox = new StackPane();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 20, 0, 20));
        titleBox.setPrefHeight(70);
        titleBox.setMaxWidth(300);
        titleBox.setBorder(new Border(new BorderStroke(
                Color.web("#ff66cc"),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(3)
        )));

        Label title = new Label("Recommended jobs");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#4664a8"));
        titleBox.getChildren().add(title);

        VBox listBox = new VBox(0);
        listBox.setPadding(new Insets(20, 0, 0, 0));

        listBox.getChildren().addAll(
                createJobRow("CBUS201 - Machine Learning TA", "MO: Kina"),
                createJobRow("EBUS204 - Operating Systems TA", "MO: Farther"),
                createJobRow("EBUS4TI - Embedded Systems TA", "MO: Naby")
        );

        jobsWrapper.getChildren().addAll(titleBox, listBox);
        return jobsWrapper;
    }

    private VBox createJobRow(String course, String teacher) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 0, 18, 0));

        Label courseLabel = new Label(course);
        courseLabel.setFont(Font.font("Arial", 22));
        courseLabel.setTextFill(Color.web("#4664a8"));
        courseLabel.setPrefWidth(520);

        Label teacherLabel = new Label(teacher);
        teacherLabel.setFont(Font.font("Arial", 22));
        teacherLabel.setTextFill(Color.web("#4664a8"));
        teacherLabel.setPrefWidth(220);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button chatButton = new Button("Chat with Teacher");
        chatButton.setPrefSize(200, 46);
        chatButton.setStyle(
                "-fx-background-color: #f0a6e9;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-size: 16px;" +
                        "-fx-background-radius: 23;" +
                        "-fx-cursor: hand;"
        );

        HBox contentRow = new HBox(20, courseLabel, teacherLabel, spacer, chatButton);
        contentRow.setAlignment(Pos.CENTER_LEFT);

        Region line = new Region();
        line.setPrefHeight(3);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setBackground(new Background(
                new BackgroundFill(Color.web("#ff8fd5"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        VBox wrapper = new VBox(10, contentRow, line);
        return wrapper;
    }

    public static void main(String[] args) {
        launch(args);
    }
}