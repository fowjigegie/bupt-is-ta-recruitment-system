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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ApplicationReviewPage extends Application {

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

        root.setTop(createNavBar("BUPT-TA", "Application Review"));
        root.setLeft(createSideBar(stage));
        root.setCenter(createMainContent());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Application Review");
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
        Button postBtn = createMenuButton("Post\nVacancies", false);
        Button manageBtn = createMenuButton("Job\nManagement", false);
        Button reviewBtn = createMenuButton("Application\nreview", true);

        dashBtn.setOnAction(e -> System.out.println("Go to dashboard"));
        postBtn.setOnAction(e -> System.out.println("Go to post vacancies"));
        manageBtn.setOnAction(e -> System.out.println("Go to job management"));
        reviewBtn.setOnAction(e -> System.out.println("Already on application review"));

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, postBtn, manageBtn, reviewBtn, filler);
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

        StackPane avatarCard = createAvatarCard();
        AnchorPane.setTopAnchor(avatarCard, 35.0);
        AnchorPane.setLeftAnchor(avatarCard, 70.0);

        VBox infoArea = createInfoArea();
        AnchorPane.setTopAnchor(infoArea, 35.0);
        AnchorPane.setLeftAnchor(infoArea, 330.0);
        AnchorPane.setRightAnchor(infoArea, 40.0);

        StackPane resumeCard = createResumeCard();
        AnchorPane.setTopAnchor(resumeCard, 290.0);
        AnchorPane.setLeftAnchor(resumeCard, 75.0);

        VBox textArea = createTextArea();
        AnchorPane.setTopAnchor(textArea, 250.0);
        AnchorPane.setLeftAnchor(textArea, 330.0);
        AnchorPane.setRightAnchor(textArea, 40.0);

        Button backBtn = createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(avatarCard, infoArea, resumeCard, textArea, backBtn);
        return main;
    }

    private StackPane createAvatarCard() {
        StackPane wrapper = new StackPane();
        wrapper.setPrefSize(180, 210);

        Rectangle frame = new Rectangle(180, 210);
        frame.setArcWidth(24);
        frame.setArcHeight(24);
        frame.setFill(Color.WHITE);
        frame.setStroke(Color.web("#f4d9e6"));
        frame.setStrokeWidth(2);

        Rectangle inner = new Rectangle(130, 160);
        inner.setArcWidth(18);
        inner.setArcHeight(18);
        inner.setFill(Color.web("#fff9f2"));
        inner.setStroke(Color.web("#ffb3d9"));
        inner.setStrokeWidth(2);

        Label icon = new Label("🙋");
        icon.setFont(Font.font("Arial", 72));

        wrapper.getChildren().addAll(frame, inner, icon);
        return wrapper;
    }

    private VBox createInfoArea() {
        VBox infoArea = new VBox(14);

        HBox row1 = new HBox(16,
                createTag("Name:   Wang Wu", 250),
                createTag("Grade: Second-year graduate", 280)
        );

        HBox row2 = new HBox(16,
                createTag("DoB:   2000-05-07", 250),
                createTag("Tel:   XXXXXX", 280)
        );

        StackPane emailTag = createTag("E-mail ad:   jp20212135XX", 546);

        infoArea.getChildren().addAll(row1, row2, emailTag);
        return infoArea;
    }

    private StackPane createTag(String text, double width) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        label.setWrapText(true);

        StackPane tag = new StackPane(label);
        tag.setAlignment(Pos.CENTER_LEFT);
        tag.setPadding(new Insets(0, 24, 0, 24));
        tag.setPrefWidth(width);
        tag.setPrefHeight(56);

        tag.setBackground(new Background(
                new BackgroundFill(Color.web("#ffb3d9"), new CornerRadii(28), Insets.EMPTY)
        ));

        return tag;
    }

    private StackPane createResumeCard() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(170, 190);

        box.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(22), Insets.EMPTY)
        ));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.DASHED,
                new CornerRadii(22),
                new BorderWidths(2)
        )));

        Label pdfIcon = new Label("PDF");
        pdfIcon.setTextFill(Color.WHITE);
        pdfIcon.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        pdfIcon.setStyle(
                "-fx-background-color: #ff4d4d;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 12 4 12;"
        );

        VBox lines = new VBox(8);
        lines.setAlignment(Pos.CENTER);
        lines.getChildren().addAll(
                createGrayLine(70),
                createGrayLine(70),
                createGrayLine(70),
                createGrayLine(55)
        );

        Label fileName = new Label("Resume.pdf");
        fileName.setTextFill(Color.web("#333333"));
        fileName.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        box.getChildren().addAll(pdfIcon, lines, fileName);
        return new StackPane(box);
    }

    private Region createGrayLine(double width) {
        Region line = new Region();
        line.setPrefSize(width, 5);
        line.setBackground(new Background(
                new BackgroundFill(Color.web("#dddddd"), new CornerRadii(3), Insets.EMPTY)
        ));
        return line;
    }

    private VBox createTextArea() {
        VBox textArea = new VBox(16);

        VBox schoolBox = createInfoBox(
                "My School Experience : I joined the college volunteer service team and participated in various campus public welfare activities and orientation services multiple times, thereby enhancing teamwork skills and sense of responsibility."
        );

        VBox advantageBox = createInfoBox(
                "My Advantages : I work meticulously and carefully, skilled at structuring and organizing information, and have clear and organized communication skills."
        );

        textArea.getChildren().addAll(schoolBox, advantageBox);
        return textArea;
    }

    private VBox createInfoBox(String text) {
        VBox box = new VBox();
        box.setPadding(new Insets(24));
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