package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
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
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginPage extends Application {

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

        HBox topBar = createNavBar("BUPT-TA", "Login page", false, stage);
        HBox bottomBar = createNavBar("BUPT-TA", "Registration page", true, stage);

        HBox mainContent = new HBox();
        mainContent.setPadding(new Insets(25, 40, 25, 40));
        mainContent.setSpacing(25);

        StackPane leftImageArea = createLeftImageArea();
        HBox.setHgrow(leftImageArea, Priority.ALWAYS);
        leftImageArea.setPrefWidth(770);

        VBox rightFormArea = createRightFormArea(stage);
        HBox.setHgrow(rightFormArea, Priority.ALWAYS);
        rightFormArea.setPrefWidth(560);

        mainContent.getChildren().addAll(leftImageArea, rightFormArea);

        root.setTop(topBar);
        root.setCenter(mainContent);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Login Page");
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(String leftText, String rightText, boolean clickableText, Stage stage) {
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
            rightLabel.setOnMouseClicked(e -> {
                try {
                    new RegisterPage().start(stage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
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

    private StackPane createLeftImageArea() {
        StackPane leftPane = new StackPane();
        leftPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-radius: 24;" +
                        "-fx-border-color: #f4d9e6;" +
                        "-fx-border-width: 1;"
        );
        leftPane.setPadding(new Insets(20));

        VBox placeholder = new VBox(18);
        placeholder.setAlignment(Pos.CENTER);

        Label title = new Label("2.5D Isometric Campus / Tech Illustration");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#8c5a6b"));

        Label subtitle = new Label("图片区占位，可替换为校园 / 科技主题插画");
        subtitle.setFont(Font.font("Arial", 16));
        subtitle.setTextFill(Color.web("#999999"));

        Rectangle block1 = new Rectangle(280, 120);
        block1.setArcWidth(25);
        block1.setArcHeight(25);
        block1.setFill(Color.web("#ffe6f0"));

        Rectangle block2 = new Rectangle(220, 90);
        block2.setArcWidth(25);
        block2.setArcHeight(25);
        block2.setFill(Color.web("#fff2cc"));

        Rectangle block3 = new Rectangle(160, 70);
        block3.setArcWidth(25);
        block3.setArcHeight(25);
        block3.setFill(Color.web("#e6f0ff"));

        VBox graphics = new VBox(15, block1, block2, block3);
        graphics.setAlignment(Pos.CENTER);

        placeholder.getChildren().addAll(title, subtitle, graphics);
        leftPane.getChildren().add(placeholder);

        return leftPane;
    }

    private VBox createRightFormArea(Stage stage) {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(30));
        container.setMaxWidth(Double.MAX_VALUE);

        Stop[] formStops = new Stop[]{
                new Stop(0, Color.web("#ffd6e8")),
                new Stop(1, Color.web("#ffcce6"))
        };
        LinearGradient formGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, formStops
        );
        container.setBackground(new Background(
                new BackgroundFill(formGradient, new CornerRadii(20), Insets.EMPTY)
        ));

        Label welcomeLabel = new Label("Welcome to log in");
        welcomeLabel.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.ITALIC, 28));
        welcomeLabel.setTextFill(Color.web("#ff66b3"));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Please enter your username");
        usernameField.setPrefHeight(50);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setStyle(
                "-fx-background-color: #fff2cc;" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0 15 0 15;" +
                        "-fx-font-size: 16px;" +
                        "-fx-prompt-text-fill: #999999;"
        );

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Please enter your password");
        passwordField.setPrefHeight(50);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffcce6, #ffb3d9);" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0 15 0 15;" +
                        "-fx-font-size: 16px;" +
                        "-fx-prompt-text-fill: #999999;"
        );

        Label roleLabel = new Label("Choose your role:");
        roleLabel.setFont(Font.font("Arial", 16));
        roleLabel.setTextFill(Color.BLACK);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("TA", "Teacher", "Student", "Admin");
        roleBox.setValue("TA");
        roleBox.setPrefWidth(240);
        roleBox.setPrefHeight(40);
        roleBox.setStyle(
                "-fx-background-color: #ffb3d9;" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-font-size: 16px;" +
                        "-fx-text-fill: white;"
        );

        Button confirmBtn = new Button("✓");
        confirmBtn.setPrefSize(40, 40);
        confirmBtn.setStyle(
                "-fx-background-color: #ff9933;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        confirmBtn.setOnAction(e -> System.out.println("当前角色: " + roleBox.getValue()));

        HBox roleRow = new HBox(12, roleBox, confirmBtn);
        roleRow.setAlignment(Pos.CENTER_LEFT);

        VBox roleSection = new VBox(10, roleLabel, roleRow);
        roleSection.setAlignment(Pos.CENTER_LEFT);
        roleSection.setMaxWidth(Double.MAX_VALUE);

        Button loginButton = new Button("Log in");
        loginButton.setPrefHeight(60);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                        "-fx-background-radius: 30;" +
                        "-fx-text-fill: #ff66b3;" +
                        "-fx-font-family: 'Comic Sans MS';" +
                        "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = roleBox.getValue();

            System.out.println("用户名: " + username);
            System.out.println("密码: " + password);
            System.out.println("角色: " + role);
        });

        Label normalText = new Label("Don’t have an account yet? ");
        normalText.setFont(Font.font("Arial", 14));
        normalText.setTextFill(Color.BLACK);

        Hyperlink signUpLink = new Hyperlink("Please sign up");
        signUpLink.setFont(Font.font("Arial", 14));
        signUpLink.setTextFill(Color.web("#3366cc"));
        signUpLink.setBorder(Border.EMPTY);
        signUpLink.setPadding(Insets.EMPTY);
        signUpLink.setOnAction(e -> {
            try {
                Stage currentStage = (Stage) signUpLink.getScene().getWindow();
                new RegisterPage().start(currentStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox signUpRow = new HBox(normalText, signUpLink);
        signUpRow.setAlignment(Pos.CENTER);

        container.getChildren().addAll(
                welcomeLabel,
                usernameField,
                passwordField,
                roleSection,
                loginButton,
                signUpRow
        );

        VBox wrapper = new VBox(container);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setFillWidth(true);
        return wrapper;
    }

    public static void main(String[] args) {
        launch(args);
    }
}