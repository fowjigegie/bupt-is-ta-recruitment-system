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

public class InterviewInvitationPage extends Application {

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

        HBox topBar = createNavBar("BUPT-TA", "Interview Invitation");
        VBox sideBar = createSideBar(stage);
        AnchorPane mainContent = createMainContent(stage);
        Region bottomBar = createBottomBar();

        root.setTop(topBar);
        root.setLeft(sideBar);
        root.setCenter(mainContent);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Interview Invitation");
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
        Button jobsBtn = createMenuButton("More Jobs", false);
        Button resumeBtn = createMenuButton("Resume\nDatabase", false);
        Button statusBtn = createMenuButton("application\nstatus", true);

        dashBtn.setOnAction(e -> System.out.println("Go to dashboard"));
        jobsBtn.setOnAction(e -> System.out.println("Go to more jobs"));
        resumeBtn.setOnAction(e -> System.out.println("Go to resume database"));
        statusBtn.setOnAction(e -> System.out.println("Already on application status"));

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

    private AnchorPane createMainContent(Stage stage) {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        VBox cardsBox = new VBox(24);
        cardsBox.getChildren().addAll(
                createInvitationCard(
                        "Kina",
                        "Sat 2026/2/28",
                        "Beijing Time 2 P.M.",
                        "IS Building room 105",
                        "Waiting"
                ),
                createInvitationCard(
                        "Mat",
                        "Thur 2026/2/12",
                        "Beijing Time 5 P.M.",
                        "https://inter_now.cn/",
                        "Waiting"
                )
        );

        AnchorPane.setTopAnchor(cardsBox, 35.0);
        AnchorPane.setLeftAnchor(cardsBox, 40.0);
        AnchorPane.setRightAnchor(cardsBox, 40.0);

        HBox pager = createPager();
        AnchorPane.setBottomAnchor(pager, 34.0);
        AnchorPane.setRightAnchor(pager, 60.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(cardsBox, pager, backBtn);
        return main;
    }

    private VBox createInvitationCard(String interviewer, String date, String time, String roomOrLink, String status) {
        VBox wrapper = new VBox();

        HBox card = new HBox(60);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setPrefHeight(210);

        card.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        card.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(1.5)
        )));

        VBox leftInfo = new VBox(16);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        leftInfo.setPrefWidth(470);

        Label interviewerLine = createInfoLine("Interviewer: ", interviewer);
        Label dateLine = createInfoLine("Date: ", date);
        Label timeLine = createInfoLine("Time: ", time);

        leftInfo.getChildren().addAll(interviewerLine, dateLine, timeLine);

        VBox rightInfo = new VBox(16);
        rightInfo.setAlignment(Pos.CENTER_LEFT);

        Label roomLine = createInfoLine("Link / Room: ", roomOrLink);

        Label statusTitle = new Label("Invitation Status");
        statusTitle.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        statusTitle.setTextFill(Color.web("#4667ad"));

        Label statusValue = new Label(status);
        statusValue.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 22));
        statusValue.setTextFill(Color.WHITE);
        statusValue.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                "-fx-background-radius: 22;" +
                "-fx-padding: 6 18 6 18;"
        );

        rightInfo.getChildren().addAll(roomLine, statusTitle, statusValue);
        card.getChildren().addAll(leftInfo, rightInfo);
        wrapper.getChildren().add(card);

        return wrapper;
    }

    private Label createInfoLine(String title, String value) {
        Label label = new Label(title + value);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        label.setTextFill(Color.web("#333333"));
        label.setWrapText(true);
        return label;
    }

    private HBox createPager() {
        HBox pager = new HBox(0);
        pager.setAlignment(Pos.CENTER);

        StackPane p1 = createPageItem("1", true, false);
        StackPane p2 = createPageItem("2", false, false);
        StackPane p3 = createPageItem("3", false, true);

        pager.getChildren().addAll(p1, p2, p3);
        return pager;
    }

    private StackPane createPageItem(String text, boolean active, boolean last) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        StackPane item = new StackPane(label);
        item.setPrefSize(56, 38);

        if (active) {
            label.setTextFill(Color.WHITE);
            item.setBackground(new Background(
                    new BackgroundFill(
                            Color.web("#ff66b3"),
                            new CornerRadii(18, 0, 0, 18, false),
                            Insets.EMPTY
                    )
            ));
        } else if (last) {
            label.setTextFill(Color.web("#4667ad"));
            item.setBackground(new Background(
                    new BackgroundFill(
                            Color.web("#ffd6e8"),
                            new CornerRadii(0, 18, 18, 0, false),
                            Insets.EMPTY
                    )
            ));
        } else {
            label.setTextFill(Color.web("#4667ad"));
            item.setBackground(new Background(
                    new BackgroundFill(Color.web("#ffd6e8"), CornerRadii.EMPTY, Insets.EMPTY)
            ));
        }

        return item;
    }

    private Button createBackButton() {
        Button backBtn = new Button("⬅");
        backBtn.setPrefSize(56, 56);
        backBtn.setStyle(
                "-fx-background-color: #b266ff;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 28;" +
                "-fx-cursor: hand;"
        );
        return backBtn;
    }

    private Region createBottomBar() {
        Region bottom = new Region();
        bottom.setPrefHeight(18);

        Stop[] bottomStops = new Stop[]{
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
        };
        LinearGradient bottomGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE, bottomStops
        );

        bottom.setBackground(new Background(
                new BackgroundFill(bottomGradient, CornerRadii.EMPTY, Insets.EMPTY)
        ));
        return bottom;
    }

    public static void main(String[] args) {
        launch(args);
    }
}