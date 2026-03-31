package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class TAWorkloadSchedulePage extends Application {

    private VBox weekPopup;
    private Button weekButton;

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

        root.setTop(createNavBar());
        root.setLeft(createSideBar());
        root.setCenter(createMainContent());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setScene(scene);
        stage.setTitle("TA Workload Schedule");
        stage.show();
    }

    private HBox createNavBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));

        bar.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ffe6e6")),
                        new Stop(1, Color.web("#ffd6d6"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        Label left = new Label("BUPT-TA");
        left.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        left.setTextFill(Color.BLACK);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label("Baron  |  Administrator");
        right.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        right.setTextFill(Color.web("#1f3b8f"));

        Polygon red = new Polygon(
                0.0, 8.0,
                8.0, 0.0,
                16.0, 8.0,
                8.0, 16.0
        );
        red.setFill(Color.web("#ff4d4d"));

        Circle purple = new Circle(9);
        purple.setFill(Color.web("#b266ff"));

        HBox rightBox = new HBox(10, right, red, purple);
        rightBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(left, spacer, rightBox);
        return bar;
    }

    private VBox createSideBar() {
        VBox side = new VBox();
        side.setPrefWidth(180);
        side.setBackground(new Background(new BackgroundFill(
                Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY
        )));

        Button dashBtn = createMenuButton("Dash Board", false);
        Button workloadBtn = createMenuButton("TA Workload", true);
        Button controlBtn = createMenuButton("Application\nControl", false);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        side.getChildren().addAll(dashBtn, workloadBtn, controlBtn, filler);
        return side;
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

        StackPane scheduleCard = createScheduleCard();
        AnchorPane.setTopAnchor(scheduleCard, 35.0);
        AnchorPane.setLeftAnchor(scheduleCard, 40.0);
        AnchorPane.setRightAnchor(scheduleCard, 40.0);
        AnchorPane.setBottomAnchor(scheduleCard, 95.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(scheduleCard, backBtn);
        return main;
    }

    private StackPane createScheduleCard() {
        StackPane stack = new StackPane();
        stack.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox();
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setSpacing(16);

        card.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(24), Insets.EMPTY
        )));
        card.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(2)
        )));

        GridPane grid = createScheduleGrid();

        card.getChildren().add(grid);

        weekPopup = createWeekPopup();
        weekPopup.setVisible(false);
        weekPopup.setManaged(false);

        StackPane.setAlignment(weekPopup, Pos.TOP_LEFT);
        StackPane.setMargin(weekPopup, new Insets(70, 180, 0, 210));

        stack.getChildren().addAll(card, weekPopup);
        return stack;
    }

    private GridPane createScheduleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setGridLinesVisible(false);

        String[] headers = {"Week", "Mon", "Tue", "Wed", "Thur", "Fri"};
        double[] widths = {110, 112, 112, 112, 112, 112};

        for (int i = 0; i < headers.length; i++) {
            StackPane headerCell;

            if (i == 0) {
                headerCell = createWeekHeaderCell();
            } else {
                headerCell = createHeaderCell(headers[i], widths[i]);
            }

            grid.add(headerCell, i, 0);
        }

        String[] times = {
                "8:00-9:00",
                "9:20-10:20",
                "10:40-11:40",
                "13:00-14:00",
                "14:20-15:20",
                "15:40-16:40",
                "17:00-18:00"
        };

        String[][] content = {
                {
                        "",
                        "EBUS204 - Operating\nSystems",
                        "",
                        "",
                        "EBUS204 - Operating\nSystems",
                        "CBUS201 - Machine\nLearning"
                },
                {
                        "",
                        "CBUS201 - Machine\nLearning\nEBUS204 - Operating...",
                        "",
                        "",
                        "EBUS204 - Operating\nSystems",
                        "CBUS201 - Machine\nLearning Test"
                },
                {
                        "",
                        "CBUS201 - Machine\nLearning",
                        "EBUS204 - Operating\nSystems Lab",
                        "",
                        "",
                        ""
                },
                {
                        "",
                        "",
                        "",
                        "CBUS201 - Machine\nLearning\nEBUS204 - Operating...",
                        "",
                        ""
                },
                {
                        "",
                        "",
                        "",
                        "CBUS201 - Machine\nLearning\nEBUS204 - Operating...",
                        "CBUS201 - Machine\nLearning",
                        ""
                },
                {
                        "",
                        "",
                        "CBUS201 - Machine\nLearning",
                        "",
                        "CBUS201 - Machine\nLearning",
                        "EBUS204 - Operating\nSystems"
                },
                {
                        "",
                        "",
                        "CBUS201 - Machine\nLearning",
                        "",
                        "",
                        "EBUS204 - Operating\nSystems Test"
                }
        };

        for (int r = 0; r < times.length; r++) {
            grid.add(createTimeCell(times[r]), 0, r + 1);

            for (int c = 1; c < headers.length; c++) {
                String text = content[r][c];
                Color textColor = Color.web("#6b84c8");

                if (text.contains("CBUS201") && text.contains("EBUS204")) {
                    textColor = Color.web("#ff6aa5");
                } else if (text.contains("CBUS201")) {
                    textColor = Color.web("#6b84c8");
                }

                grid.add(createBodyCell(text, widths[c], textColor), c, r + 1);
            }
        }

        return grid;
    }

    private StackPane createWeekHeaderCell() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(110, 54);
        box.setBackground(new Background(new BackgroundFill(
                Color.web("#dfe6ff"), CornerRadii.EMPTY, Insets.EMPTY
        )));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#a8b8ff"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));

        Label text = new Label("Week");
        text.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        text.setTextFill(Color.web("#333333"));

        weekButton = new Button("▼");
        weekButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        weekButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #ff66b3;" +
                "-fx-cursor: hand;"
        );

        weekButton.setOnAction(e -> {
            boolean show = !weekPopup.isVisible();
            weekPopup.setVisible(show);
            weekPopup.setManaged(show);
            weekButton.setText(show ? "▲" : "▼");
        });

        box.getChildren().addAll(text, weekButton);
        return new StackPane(box);
    }

    private StackPane createHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#333333"));

        StackPane cell = new StackPane(label);
        cell.setPrefSize(width, 54);
        cell.setBackground(new Background(new BackgroundFill(
                Color.web("#dfe6ff"), CornerRadii.EMPTY, Insets.EMPTY
        )));
        cell.setBorder(new Border(new BorderStroke(
                Color.web("#a8b8ff"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));
        return cell;
    }

    private StackPane createTimeCell(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        label.setTextFill(Color.web("#333333"));

        StackPane cell = new StackPane(label);
        cell.setPrefSize(110, 74);
        cell.setBackground(new Background(new BackgroundFill(
                Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY
        )));
        cell.setBorder(new Border(new BorderStroke(
                Color.web("#a8b8ff"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));
        return cell;
    }

    private StackPane createBodyCell(String text, double width, Color textColor) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(textColor);
        label.setMaxWidth(width - 12);

        StackPane cell = new StackPane(label);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setPadding(new Insets(8));
        cell.setPrefSize(width, 74);
        cell.setBackground(new Background(new BackgroundFill(
                Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY
        )));
        cell.setBorder(new Border(new BorderStroke(
                Color.web("#a8b8ff"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1)
        )));
        return cell;
    }

    private VBox createWeekPopup() {
        VBox popup = new VBox(22);
        popup.setAlignment(Pos.CENTER);
        popup.setPadding(new Insets(18, 24, 22, 24));
        popup.setPrefWidth(520);

        popup.setBackground(new Background(new BackgroundFill(
                Color.web("#ffe6f2"), new CornerRadii(26), Insets.EMPTY
        )));
        popup.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"), BorderStrokeStyle.SOLID, new CornerRadii(26), new BorderWidths(1.5)
        )));

        Label title = new Label("Choose a Week");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#333333"));

        GridPane weeksGrid = new GridPane();
        weeksGrid.setHgap(18);
        weeksGrid.setVgap(18);
        weeksGrid.setAlignment(Pos.CENTER);

        int num = 1;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                Button week = createWeekCircleButton(String.valueOf(num));
                weeksGrid.add(week, col, row);
                num++;
            }
        }

        popup.getChildren().addAll(title, weeksGrid);
        return popup;
    }

    private Button createWeekCircleButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(42, 42);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        btn.setStyle(
                "-fx-background-color: #ff66a3;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 21;" +
                "-fx-cursor: hand;"
        );

        btn.setOnAction(e -> {
            weekPopup.setVisible(false);
            weekPopup.setManaged(false);
            weekButton.setText("▼");
        });

        return btn;
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