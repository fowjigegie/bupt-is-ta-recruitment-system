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

public class MessagesPage extends Application {

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

        root.setTop(createNavBar("BUPT-TA", "Messages"));
        root.setLeft(createSideBar(stage));
        root.setCenter(createMainContent());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Messages");
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

        Button dashBtn = createMenuButton("Dash Board", true);
        Button jobsBtn = createMenuButton("More Jobs", false);
        Button resumeBtn = createMenuButton("Resume\nDatabase", false);
        Button statusBtn = createMenuButton("application\nstatus", false);

        dashBtn.setOnAction(e -> System.out.println("Already on dashboard/messages area"));
        jobsBtn.setOnAction(e -> System.out.println("Go to more jobs"));
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

        VBox listBox = new VBox(18);
        listBox.getChildren().addAll(
                createMessageRow("Kina", "Re: You need to check your e-mails regularly...", true, 2, false),
                createMessageRow("Naby", "Re: You have to complete your personal info...", false, -1, true),
                createMessageRow("Cindy", "Sent: I am going to the supermarket.", false, -1, false),
                createMessageRow("Baron (admin)", "Sent: Thank you very much.", false, -1, false),
                createMessageRow("Mat", "Re: It seems that you are not suitable for this...", false, -1, false)
        );

        AnchorPane.setTopAnchor(listBox, 30.0);
        AnchorPane.setLeftAnchor(listBox, 40.0);
        AnchorPane.setRightAnchor(listBox, 40.0);

        HBox pager = createPager();
        AnchorPane.setBottomAnchor(pager, 34.0);
        AnchorPane.setRightAnchor(pager, 60.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(listBox, pager, backBtn);
        return main;
    }

    private HBox createMessageRow(String name, String preview, boolean unreadCountVisible, int unreadCount, boolean redDot) {
        HBox outer = new HBox();
        outer.setAlignment(Pos.CENTER_LEFT);

        StackPane wrapper = new StackPane();
        wrapper.setPrefHeight(92);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        Region cardBg = new Region();
        cardBg.setPrefHeight(92);
        cardBg.setMaxWidth(Double.MAX_VALUE);
        cardBg.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)
        ));
        cardBg.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(24),
                new BorderWidths(1.5)
        )));

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 22, 0, 22));

        StackPane avatar = createAvatar();

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.web("#333333"));
        nameLabel.setMinWidth(145);

        Region divider = new Region();
        divider.setPrefWidth(3);
        divider.setPrefHeight(46);
        divider.setBackground(new Background(
                new BackgroundFill(Color.web("#ffe6f0"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Label previewLabel = new Label(preview);
        previewLabel.setFont(Font.font("Arial", 18));
        previewLabel.setTextFill(Color.web("#9a8aa0"));

        row.getChildren().addAll(avatar, nameLabel, divider, previewLabel);

        wrapper.getChildren().addAll(cardBg, row);
        StackPane.setAlignment(row, Pos.CENTER_LEFT);

        if (unreadCountVisible && unreadCount >= 0) {
            Circle badgeCircle = new Circle(18, Color.web("#ff4d4d"));
            Label badgeLabel = new Label(String.valueOf(unreadCount));
            badgeLabel.setTextFill(Color.WHITE);
            badgeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

            StackPane badge = new StackPane(badgeCircle, badgeLabel);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            badge.setTranslateX(-18);
            badge.setTranslateY(8);
            wrapper.getChildren().add(badge);
        } else if (redDot) {
            Circle dot = new Circle(10, Color.web("#ff4d4d"));
            StackPane.setAlignment(dot, Pos.TOP_RIGHT);
            dot.setTranslateX(-22);
            dot.setTranslateY(12);
            wrapper.getChildren().add(dot);
        }

        HBox.setHgrow(wrapper, Priority.ALWAYS);
        outer.getChildren().add(wrapper);
        return outer;
    }

    private StackPane createAvatar() {
        Circle outerCircle = new Circle(28);
        outerCircle.setFill(Color.web("#ffd6e8"));

        Circle head = new Circle(7);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(Color.web("#ff66b3"));
        head.setStrokeWidth(3);
        head.setTranslateY(-9);

        Circle body = new Circle(12);
        body.setFill(Color.TRANSPARENT);
        body.setStroke(Color.web("#ff66b3"));
        body.setStrokeWidth(3);
        body.setScaleY(0.7);
        body.setTranslateY(10);

        StackPane avatar = new StackPane(outerCircle, body, head);
        avatar.setPrefSize(56, 56);
        return avatar;
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