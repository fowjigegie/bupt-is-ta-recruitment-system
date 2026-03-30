package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ResumeDatabasePage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#fff8fb")),
                new Stop(1, Color.web("#fffdf5"))
        };
        LinearGradient pageBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops
        );
        root.setBackground(new Background(new BackgroundFill(pageBg, CornerRadii.EMPTY, Insets.EMPTY)));

        HBox topBar = createTopBar();
        VBox leftMenu = createLeftMenu();
        BorderPane centerArea = createCenterArea();

        root.setTop(topBar);
        root.setLeft(leftMenu);
        root.setCenter(centerArea);

        Scene scene = new Scene(root, 1360, 830);
        stage.setTitle("BUPT-TA Resume Database");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 28, 16, 28));

        Stop[] navStops = new Stop[]{
                new Stop(0, Color.web("#f7edb5")),
                new Stop(0.55, Color.web("#f9d9df")),
                new Stop(1, Color.web("#d79af7"))
        };
        LinearGradient navBg = new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE, navStops
        );
        topBar.setBackground(new Background(new BackgroundFill(navBg, CornerRadii.EMPTY, Insets.EMPTY)));

        Label leftTitle = new Label("BUPT-TA");
        leftTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        leftTitle.setTextFill(Color.BLACK);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label bellIcon = new Label("🔔");
        bellIcon.setFont(Font.font(22));

        Label rightTitle = new Label("LI HUA");
        rightTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        rightTitle.setTextFill(Color.web("#1f3d8f"));

        Label dept = new Label("computer department");
        dept.setFont(Font.font("Arial", 14));
        dept.setTextFill(Color.web("#3f6cc0"));

        VBox rightTextGroup = new VBox(2, rightTitle, dept);
        rightTextGroup.setAlignment(Pos.CENTER_LEFT);

        Circle starCircle = new Circle(22);
        starCircle.setFill(Color.web("#a15be8"));
        Label starLabel = new Label("★");
        starLabel.setTextFill(Color.web("#3b2d86"));
        starLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        StackPane starPane = new StackPane(starCircle, starLabel);

        HBox rightGroup = new HBox(12, bellIcon, rightTextGroup, starPane);
        rightGroup.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(leftTitle, spacer, rightGroup);
        return topBar;
    }

    private VBox createLeftMenu() {
        VBox menu = new VBox();
        menu.setPrefWidth(180);
        menu.setPadding(new Insets(0));
        menu.setStyle("-fx-background-color: #f5b4e6;");

        Button dashBtn = createMenuButton("Dash Board", false);
        Button jobsBtn = createMenuButton("More Jobs", false);
        Button resumeBtn = createMenuButton("Resume\nDatabase", true);
        Button statusBtn = createMenuButton("application\nstatus", false);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        menu.getChildren().addAll(dashBtn, jobsBtn, resumeBtn, statusBtn, filler);
        return menu;
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

    private BorderPane createCenterArea() {
        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(24, 32, 24, 32));

        VBox content = new VBox(18);
        content.getChildren().addAll(
                createResumeTabs(),
                createFormSection(),
                createBottomHelperRow()
        );

        centerPane.setCenter(content);
        return centerPane;
    }

    private HBox createResumeTabs() {
        HBox tabRow = new HBox(25);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        Button resume1 = new Button("Resume1");
        resume1.setStyle(
                "-fx-background-color: #f56bc7;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-background-radius: 24 24 0 0;" +
                "-fx-padding: 12 28 12 28;" +
                "-fx-cursor: hand;"
        );

        Button resume2 = new Button("Resume2");
        resume2.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #34539c;" +
                "-fx-font-size: 18px;" +
                "-fx-cursor: hand;"
        );

        Button resume3 = new Button("Resume3");
        resume3.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #34539c;" +
                "-fx-font-size: 18px;" +
                "-fx-cursor: hand;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addResume = new Button("+ add resume");
        addResume.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #c45be7;" +
                "-fx-font-size: 18px;" +
                "-fx-cursor: hand;"
        );

        tabRow.getChildren().addAll(resume1, resume2, resume3, spacer, addResume);
        return tabRow;
    }

    private HBox createFormSection() {
        HBox mainRow = new HBox(28);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox leftForm = new VBox(16);
        leftForm.setPrefWidth(700);

        HBox row1 = new HBox(24);
        TextField nameField = createRoundedField("Name:", 260);
        ComboBox<String> gradeBox = createGradeBox();

        VBox gradeBoxWrapper = new VBox();
        gradeBoxWrapper.getChildren().add(gradeBox);

        row1.getChildren().addAll(nameField, gradeBoxWrapper);

        HBox row2 = new HBox(24);
        TextField dobField = createRoundedField("DoB:", 260);
        TextField telField = createRoundedField("Tel:", 260);
        row2.getChildren().addAll(dobField, telField);

        TextField emailField = createRoundedField("E-mail ad:", 544);

        TextArea schoolExpArea = createLargeTextArea("My School Experience :");
        TextArea advantagesArea = createLargeTextArea("My Advantages :");

        leftForm.getChildren().addAll(row1, row2, emailField, schoolExpArea, advantagesArea);

        VBox rightPanel = new VBox(18);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPrefWidth(280);

        StackPane avatarPane = createAvatarPane();

        Label completionLabel = new Label("The completeness of the resume");
        completionLabel.setFont(Font.font("Arial", 14));
        completionLabel.setTextFill(Color.web("#4a63af"));

        ProgressBar progressBar = new ProgressBar(0.60);
        progressBar.setPrefWidth(220);
        progressBar.setPrefHeight(18);
        progressBar.setStyle(
                "-fx-accent: #f15bbe;" +
                "-fx-control-inner-background: #f8c4ea;"
        );

        Label progressText = new Label("60%");
        progressText.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        progressText.setTextFill(Color.WHITE);
        progressText.setStyle(
                "-fx-background-color: #f15bbe;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 4 14 4 14;"
        );

        StackPane progressStack = new StackPane(progressBar, progressText);
        progressStack.setAlignment(Pos.CENTER);

        StackPane uploadBox = new StackPane();
        uploadBox.setPrefSize(220, 110);
        uploadBox.setStyle(
                "-fx-border-color: #f4a6dc;" +
                "-fx-border-width: 3;" +
                "-fx-border-style: segments(8, 8);" +
                "-fx-background-color: transparent;"
        );

        Label uploadLabel = new Label("Upload here...");
        uploadLabel.setFont(Font.font("Arial", 16));
        uploadLabel.setTextFill(Color.web("#4d6ab2"));
        uploadBox.getChildren().add(uploadLabel);

        Button matchBtn = new Button("Match & analyze\nyour TA skills!");
        matchBtn.setPrefSize(190, 58);
        matchBtn.setStyle(
                "-fx-background-color: white;" +
                "-fx-text-fill: black;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 28;" +
                "-fx-border-color: black;" +
                "-fx-border-radius: 28;" +
                "-fx-border-width: 2;" +
                "-fx-cursor: hand;"
        );

        Button chatBtn = new Button("Click here to chat");
        chatBtn.setPrefSize(160, 46);
        chatBtn.setStyle(
                "-fx-background-color: white;" +
                "-fx-text-fill: black;" +
                "-fx-font-size: 15px;" +
                "-fx-background-radius: 25;" +
                "-fx-border-color: #d66adf;" +
                "-fx-border-radius: 25;" +
                "-fx-cursor: hand;"
        );

        Label robot = new Label("🤖");
        robot.setFont(Font.font(60));

        HBox robotRow = new HBox(16, robot, chatBtn);
        robotRow.setAlignment(Pos.CENTER);

        rightPanel.getChildren().addAll(
                avatarPane,
                completionLabel,
                progressStack,
                uploadBox,
                matchBtn,
                robotRow
        );

        mainRow.getChildren().addAll(leftForm, rightPanel);
        return mainRow;
    }

    private TextField createRoundedField(String prompt, double width) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(width);
        field.setPrefHeight(58);
        field.setFont(Font.font("Arial", 16));
        field.setStyle(
                "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-border-color: transparent;" +
                "-fx-prompt-text-fill: white;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0 18 0 18;"
        );
        return field;
    }

    private ComboBox<String> createGradeBox() {
        ComboBox<String> gradeBox = new ComboBox<>();
        gradeBox.getItems().addAll(
                "Senior undergraduate",
                "First-year graduate",
                "Second-year graduate",
                "Third-year graduate"
        );
        gradeBox.setValue("Senior undergraduate");
        gradeBox.setPrefWidth(260);
        gradeBox.setPrefHeight(58);
        gradeBox.setStyle(
                "-fx-background-color: #eba8df;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-font-size: 16px;" +
                "-fx-padding: 0 12 0 12;"
        );
        return gradeBox;
    }

    private TextArea createLargeTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefWidth(544);
        area.setPrefHeight(130);
        area.setWrapText(true);
        area.setFont(Font.font("Arial", 16));
        area.setStyle(
                "-fx-control-inner-background: white;" +
                "-fx-background-color: white;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #f3b2df;" +
                "-fx-border-width: 3;" +
                "-fx-prompt-text-fill: black;" +
                "-fx-padding: 12;"
        );
        return area;
    }

    private StackPane createAvatarPane() {
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(110, 130);

        Rectangle outer = new Rectangle(80, 95);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#db4b87"));
        outer.setStrokeWidth(3);

        Label avatarIcon = new Label("👤");
        avatarIcon.setFont(Font.font(42));
        avatarIcon.setTextFill(Color.web("#db4b87"));

        Button editBtn = new Button("edit");
        editBtn.setStyle(
                "-fx-background-color: #db4b87;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 0;" +
                "-fx-cursor: hand;"
        );
        editBtn.setPrefWidth(72);

        VBox box = new VBox(8, new StackPane(outer, avatarIcon), editBtn);
        box.setAlignment(Pos.CENTER);

        avatarPane.getChildren().add(box);
        return avatarPane;
    }

    private HBox createBottomHelperRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label arrow = new Label("⬅");
        arrow.setFont(Font.font(38));
        arrow.setTextFill(Color.web("#c45be7"));

        row.getChildren().add(arrow);
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}