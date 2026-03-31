package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ApplicationStatusPage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // 页面背景 - 严格按照你给的 LoginPage 写法
        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#fff5f8")),
                new Stop(1, Color.web("#fff9e6"))
        };
        LinearGradient pageBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops
        );
        root.setBackground(new Background(new BackgroundFill(pageBg, CornerRadii.EMPTY, Insets.EMPTY)));

        root.setTop(createTopBar(stage));
        root.setLeft(createSideBar(stage));
        root.setCenter(createMainContent(stage));

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA - Application Status");
        stage.setScene(scene);
        stage.show();
    }

    // --------------- 以下方法全部复制你截图里的写法 ---------------
    private HBox createTopBar(Stage stage) {
        HBox navBar = new HBox();
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(12, 24, 12, 24));
        navBar.setPrefHeight(70);

        // 顶部导航栏渐变
        Stop[] navStops = new Stop[]{
                new Stop(0, Color.web("#ffe6e6")),
                new Stop(1, Color.web("#ffd6d6"))
        };
        LinearGradient navGradient = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE, navStops
        );
        navBar.setBackground(new Background(new BackgroundFill(navGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Label leftLabel = new Label("BUPT-TA");
        leftLabel.setTextFill(Color.BLACK);
        leftLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 红色铃铛图标
        Polygon redBell = new Polygon(
                10.0, 0.0, 16.0, 8.0, 16.0, 22.0,
                8.0, 30.0, 0.0, 22.0, 0.0, 8.0
        );
        redBell.setFill(Color.web("#ff2d2d"));

        Region divider = new Region();
        divider.setPrefSize(2, 40);
        divider.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        // 用户信息
        Label userInfo = new Label("LI HUA\ncomputer department");
        userInfo.setTextFill(Color.web("#315ea8"));
        userInfo.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // 紫色星星圆圈
        Circle starCircle = new Circle(20);
        starCircle.setFill(Color.web("#b266ff"));
        Label star = new Label("★");
        star.setTextFill(Color.web("#315ea8"));
        star.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        StackPane starPane = new StackPane(starCircle, star);

        HBox rightGroup = new HBox(10, redBell, divider, userInfo, starPane);
        rightGroup.setAlignment(Pos.CENTER);

        navBar.getChildren().addAll(leftLabel, spacer, rightGroup);
        return navBar;
    }

    private VBox createSideBar(Stage stage) {
        VBox side = new VBox(0);
        side.setPrefWidth(180);
        // 侧栏背景色 - 严格看你截图：#ffb3d9
        side.setBackground(new Background(new BackgroundFill(Color.web("#ffb3d9"), CornerRadii.EMPTY, Insets.EMPTY)));

        side.getChildren().addAll(
                createMenuItem("Dash Board", false, stage),
                createMenuItem("More Jobs", false, stage),
                createMenuItem("Resume\nDatabase", false, stage),
                createMenuItem("Application\nStatus", true, stage),
                createSideFill(),
                createBackArrow(stage)
        );
        return side;
    }

    private StackPane createMenuItem(String text, boolean active, Stage stage) {
        Label label = new Label(text);
        label.setWrapText(true);
        StackPane pane = new StackPane(label);
        pane.setPrefHeight(70);
        pane.setPadding(new Insets(0, 0, 0, 20));

        if (active) {
            // 选中状态 - 你截图里的颜色：#4969ad
            label.setTextFill(Color.WHITE);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            pane.setBackground(new Background(new BackgroundFill(
                    Color.web("#4969ad"),
                    new CornerRadii(0, 30, 30, 0),
                    Insets.EMPTY
            )));
        } else {
            // 未选中状态
            label.setTextFill(Color.WHITE);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            pane.setBorder(new Border(new BorderStroke(
                    Color.web("#ffd6ec"),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0, 0, 1, 0)
            )));
        }
        return pane;
    }

    private Region createSideFill() {
        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);
        return fill;
    }

    private StackPane createBackArrow(Stage stage) {
        Label arrow = new Label("⬅");
        arrow.setTextFill(Color.web("#b965ef"));
        arrow.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        StackPane box = new StackPane(arrow);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox createMainContent(Stage stage) {
        VBox main = new VBox(30);
        main.setPadding(new Insets(40));
        main.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("My Application Status");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#4969ad"));

        VBox cardList = new VBox(20);
        cardList.setAlignment(Pos.TOP_LEFT);
        cardList.getChildren().addAll(
                createStatusCard("Computer Network", "Pending Review", "#ff66b3"),
                createStatusCard("Data Structure", "Interview Scheduled", "#4969ad"),
                createStatusCard("Algorithm", "Accepted", "#2ecc71"),
                createStatusCard("AI Laboratory", "Rejected", "#e74c3c")
        );

        main.getChildren().addAll(title, cardList);
        return main;
    }

    // 单个状态卡片
    private HBox createStatusCard(String jobName, String status, String color) {
        HBox card = new HBox(25);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 30, 22, 30));
        card.setPrefWidth(900);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(20), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
                Color.web(color),
                BorderStrokeStyle.SOLID,
                new CornerRadii(20),
                new BorderWidths(3)
        )));

        Label jobLabel = new Label(jobName);
        jobLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(status);
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.web(color));

        Button btn = new Button("View Details");
        btn.setPrefSize(130, 40);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 20;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );
        btn.setOnAction(e -> System.out.println("Open: " + jobName));

        card.getChildren().addAll(jobLabel, spacer, statusLabel, btn);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}