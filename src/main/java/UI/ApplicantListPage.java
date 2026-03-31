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

public class ApplicantListPage extends Application {

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
        stage.setTitle("BUPT-TA Applicant List");
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
        Button controlBtn = createMenuButton("Application\nControl", true);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, workloadBtn, controlBtn, filler);
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

        VBox tableCard = createTableCard();
        AnchorPane.setTopAnchor(tableCard, 105.0);
        AnchorPane.setLeftAnchor(tableCard, 40.0);
        AnchorPane.setRightAnchor(tableCard, 40.0);

        HBox pager = createPager();
        AnchorPane.setBottomAnchor(pager, 34.0);
        AnchorPane.setRightAnchor(pager, 60.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(searchBar, tableCard, pager, backBtn);
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

        Label text = new Label("TA Username / Job / Status ...");
        text.setTextFill(Color.WHITE);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        bar.getChildren().addAll(searchIcon, line, text);
        return new StackPane(bar);
    }

    private VBox createTableCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 18, 16, 18));

        card.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        card.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(2)
        )));

        HBox header = createHeaderRow();

        VBox rows = new VBox(10);
        rows.getChildren().addAll(
                createDataRow("Li Hua", "BUPT CS Master", "CBUS201 ML TA", "2026-03-14", "Applied"),
                createDataRow("Zhang San", "ECE Master", "CBUS201 ML TA\nEBUS477 Embedded TA", "2026-03-13", "Assessing"),
                createDataRow("Wang Wu", "BUPT CS Master", "CBUS201 ML TA", "2026-03-13", "Assessing"),
                createDataRow("Liu Min", "BUPT AI Master", "CBUS201 ML TA\nEBUS501 - Middleware TA", "2026-03-10", "Hired")
        );

        card.getChildren().addAll(header, rows);
        return card;
    }

    private HBox createHeaderRow() {
        HBox header = new HBox(10);
        header.getChildren().addAll(
                createHeaderCell("Name", 140),
                createHeaderCell("Major", 180),
                createHeaderCell("Job", 220),
                createHeaderCell("Start Date", 160),
                createHeaderCell("Status", 140)
        );
        return header;
    }

    private StackPane createHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setTextFill(Color.web("#e05a92"));

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setPrefSize(width, 52);
        cell.setBackground(new Background(
                new BackgroundFill(Color.web("#ffd6e8"), new CornerRadii(0), Insets.EMPTY)
        ));
        return cell;
    }

    private HBox createDataRow(String name, String major, String job, String startDate, String status) {
        HBox row = new HBox(10);
        row.getChildren().addAll(
                createBodyCell(name, 140),
                createBodyCell(major, 180),
                createBodyCell(job, 220),
                createBodyCell(startDate, 160),
                createStatusCell(status, 140)
        );
        return row;
    }

    private StackPane createBodyCell(String text, double width) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#4664a8"));

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(8));
        cell.setPrefWidth(width);
        cell.setMinHeight(56);
        cell.setBackground(new Background(
                new BackgroundFill(Color.web("#fff3f7"), CornerRadii.EMPTY, Insets.EMPTY)
        ));
        return cell;
    }

    private StackPane createStatusCell(String text, double width) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#4664a8"));

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(8));
        cell.setPrefWidth(width);
        cell.setMinHeight(56);
        cell.setBackground(new Background(
                new BackgroundFill(Color.web("#fff3f7"), CornerRadii.EMPTY, Insets.EMPTY)
        ));
        return cell;
    }

    private HBox createPager() {
        HBox pager = new HBox(0);
        pager.setAlignment(Pos.CENTER);

        StackPane p1 = createPageItem("1", true, false);
        StackPane p2 = createPageItem("2", false, false);
        StackPane p3 = createPageItem("3", false, false);
        StackPane p4 = createPageItem("4", false, true);

        pager.getChildren().addAll(p1, p2, p3, p4);
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