package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class JobManagementPage extends Application {

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
        root.setBackground(new Background(
                new BackgroundFill(pageBg, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        root.setTop(createNavBar());
        root.setLeft(createSideBar());
        root.setCenter(createMainContent());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Job Management");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar() {
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
        navBar.setBackground(new Background(
                new BackgroundFill(navGradient, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Label leftLabel = new Label("BUPT-TA");
        leftLabel.setTextFill(Color.BLACK);
        leftLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rightLabel = new Label("Baron  |  Administrator");
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

    private VBox createSideBar() {
        VBox sideBar = new VBox();
        sideBar.setPrefWidth(180);
        sideBar.setBackground(new Background(
                new BackgroundFill(Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Button dashBtn = createMenuButton("Dash Board", false);
        Button workloadBtn = createMenuButton("TA Workload", false);
        Button controlBtn = createMenuButton("Application\nControl", false);
        Button jobBtn = createMenuButton("Job\nManagement", true);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, workloadBtn, controlBtn, jobBtn, filler);
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

        StackPane searchBar = createSearchBar();
        AnchorPane.setTopAnchor(searchBar, 28.0);
        AnchorPane.setLeftAnchor(searchBar, 40.0);
        AnchorPane.setRightAnchor(searchBar, 40.0);

        StackPane timeButton = createTimeButton();
        AnchorPane.setTopAnchor(timeButton, 98.0);
        AnchorPane.setLeftAnchor(timeButton, 300.0);

        VBox tableCard = createTableCard();
        AnchorPane.setTopAnchor(tableCard, 150.0);
        AnchorPane.setLeftAnchor(tableCard, 40.0);
        AnchorPane.setRightAnchor(tableCard, 40.0);

        HBox pager = createPager();
        AnchorPane.setBottomAnchor(pager, 34.0);
        AnchorPane.setRightAnchor(pager, 60.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(searchBar, timeButton, tableCard, pager, backBtn);
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
                new Stop(1, Color.web("#c68cff"))
        );
        bar.setBackground(new Background(
                new BackgroundFill(gradient, new CornerRadii(28), Insets.EMPTY)
        ));

        Label searchIcon = new Label("⌕");
        searchIcon.setTextFill(Color.WHITE);
        searchIcon.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Region line = new Region();
        line.setPrefWidth(3);
        line.setPrefHeight(32);
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
        box.setPrefSize(120, 42);
        box.setBackground(new Background(
                new BackgroundFill(Color.web("#ffd6e8"), new CornerRadii(21), Insets.EMPTY)
        ));

        Label text = new Label("Time");
        text.setTextFill(Color.web("#4667ad"));
        text.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Label arrow = new Label("▼");
        arrow.setTextFill(Color.web("#ff66b3"));
        arrow.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        box.getChildren().addAll(text, arrow);
        return new StackPane(box);
    }

    private VBox createTableCard() {
        VBox card = new VBox();
        card.setPadding(new Insets(18, 28, 18, 28));
        card.setSpacing(0);

        card.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        card.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(2)
        )));

        HBox header = createHeader();
        VBox rows = new VBox(0);

        rows.getChildren().addAll(
                createJobRow("CBUS201 - Machine Learning TA", "Status: Open | Applications: 5", "Kina"),
                createJobRow("EBUS204 - Operating Systems TA", "Status: Open | Applications: 10", "Farther"),
                createJobRow("EBUS471 - Embedded Systems TA", "Status: Open | Applications: 12", "Naby"),
                createJobRow("EBUS212 - Communications and Networks TA", "Status: Open | Applications: 9", "Lary"),
                createJobRow("EBUS501 - Middleware TA", "Status: Open | Applications: 4", "Monica")
        );

        card.getChildren().addAll(header, rows);
        return card;
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 10, 12, 10));

        Label jobLabel = createHeaderLabel("Job", 640);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label posterLabel = createHeaderLabel("Poster", 160);

        header.getChildren().addAll(jobLabel, spacer, posterLabel);
        return header;
    }

    private Label createHeaderLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        label.setTextFill(Color.web("#4664a8"));
        return label;
    }

    private VBox createJobRow(String job, String status, String poster) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 10, 16, 10));

        VBox leftBox = new VBox(6);
        leftBox.setPrefWidth(640);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Label jobLabel = new Label(job);
        jobLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        jobLabel.setTextFill(Color.web("#4664a8"));

        Label statusLabel = new Label(status);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.web("#4664a8"));

        leftBox.getChildren().addAll(jobLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label posterLabel = new Label(poster);
        posterLabel.setPrefWidth(160);
        posterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        posterLabel.setTextFill(Color.web("#4664a8"));
        posterLabel.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(leftBox, spacer, posterLabel);

        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setBackground(new Background(
                new BackgroundFill(Color.web("#ffd6e8"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        VBox wrapper = new VBox(row, separator);
        return wrapper;
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
        item.setPrefSize(48, 36);

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