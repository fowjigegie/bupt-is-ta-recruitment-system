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

public class MoreJobsPage extends Application {

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

        root.setTop(createNavBar("BUPT-TA", "More Jobs"));
        root.setLeft(createSideBar(stage));
        root.setCenter(createMainContent(stage));
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA More Jobs");
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

    private AnchorPane createMainContent(Stage stage) {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        StackPane searchBar = createSearchBar();
        AnchorPane.setTopAnchor(searchBar, 28.0);
        AnchorPane.setLeftAnchor(searchBar, 40.0);
        AnchorPane.setRightAnchor(searchBar, 160.0);

        StackPane timeButton = createTimeButton();
        AnchorPane.setTopAnchor(timeButton, 28.0);
        AnchorPane.setRightAnchor(timeButton, 40.0);

        VBox jobsList = new VBox(16);
        jobsList.getChildren().addAll(
                createJobCard("EBUS204 - Operating Systems TA", "MO : Farther", "View Details", "Chat with MO", false, false),
                createJobCard("CBUS201 - Machine Learning TA", "MO : Kina", "View Details", "View Chat History", true, false),
                createJobCard("EBUS471 - Embedded Systems TA", "MO : Naby", "View Details", "View Chat History", false, true),
                createJobCard("EBUS212 - Communications and Networks TA", "MO : Lary", "View Details", "Chat with MO", false, false),
                createJobCard("EBUS601 - Middleware TA", "MO : Monica", "View Details", "Chat with MO", false, false)
        );

        AnchorPane.setTopAnchor(jobsList, 110.0);
        AnchorPane.setLeftAnchor(jobsList, 40.0);
        AnchorPane.setRightAnchor(jobsList, 40.0);

        HBox pager = createPager();
        AnchorPane.setBottomAnchor(pager, 34.0);
        AnchorPane.setRightAnchor(pager, 60.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(searchBar, timeButton, jobsList, pager, backBtn);
        return main;
    }

    private StackPane createSearchBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 22, 0, 22));
        bar.setPrefHeight(56);

        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffcce6")),
                new Stop(1, Color.web("#ffb3d9"))
        );
        bar.setBackground(new Background(
                new BackgroundFill(gradient, new CornerRadii(28), Insets.EMPTY)
        ));

        Label searchIcon = new Label("⌕");
        searchIcon.setTextFill(Color.WHITE);
        searchIcon.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Region line = new Region();
        line.setPrefWidth(3);
        line.setPrefHeight(34);
        line.setBackground(new Background(
                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Label text = new Label("Course id / Course Name / MO Name ...");
        text.setTextFill(Color.WHITE);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        bar.getChildren().addAll(searchIcon, line, text);
        return new StackPane(bar);
    }

    private StackPane createTimeButton() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(140, 50);
        box.setBackground(new Background(
                new BackgroundFill(Color.web("#ffd6e8"), new CornerRadii(25), Insets.EMPTY)
        ));

        Label text = new Label("Time");
        text.setTextFill(Color.web("#4667ad"));
        text.setFont(Font.font("Arial", FontWeight.BOLD, 17));

        Label arrow = new Label("▼");
        arrow.setTextFill(Color.web("#ff66b3"));
        arrow.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        box.getChildren().addAll(text, arrow);
        return new StackPane(box);
    }

    private VBox createJobCard(String course, String mo, String leftBtnText, String rightBtnText,
                               boolean badgeNumber, boolean badgeDot) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 24, 20, 24));
        row.setSpacing(20);

        row.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        row.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(1.5)
        )));

        VBox textBox = new VBox(6);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPrefWidth(500);

        Label courseLabel = new Label(course);
        courseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        courseLabel.setTextFill(Color.web("#4664a8"));

        Label moLabel = new Label(mo);
        moLabel.setFont(Font.font("Arial", 17));
        moLabel.setTextFill(Color.web("#8b7fa0"));

        textBox.getChildren().addAll(courseLabel, moLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewDetails = createPinkButton(leftBtnText, 150, 44);

        StackPane rightButtonWrap = new StackPane();
        Button rightBtn = createPinkButton(rightBtnText, 180, 44);
        rightButtonWrap.getChildren().add(rightBtn);

        if (badgeNumber) {
            Circle badgeCircle = new Circle(14, Color.web("#ff4d4d"));
            Label badgeLabel = new Label("2");
            badgeLabel.setTextFill(Color.WHITE);
            badgeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
            StackPane badge = new StackPane(badgeCircle, badgeLabel);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            badge.setTranslateX(8);
            badge.setTranslateY(-8);
            rightButtonWrap.getChildren().add(badge);
        }

        if (badgeDot) {
            Circle dot = new Circle(8, Color.web("#ff4d4d"));
            StackPane.setAlignment(dot, Pos.TOP_RIGHT);
            dot.setTranslateX(8);
            dot.setTranslateY(-8);
            rightButtonWrap.getChildren().add(dot);
        }

        HBox buttonBox = new HBox(16, viewDetails, rightButtonWrap);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(textBox, spacer, buttonBox);

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    private Button createPinkButton(String text, double width, double height) {
        Button btn = new Button(text);
        btn.setPrefSize(width, height);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        btn.setTextFill(Color.web("#333333"));
        btn.setStyle(
                "-fx-background-color: #ffd6e8;" +
                "-fx-background-radius: 22;" +
                "-fx-cursor: hand;"
        );
        return btn;
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