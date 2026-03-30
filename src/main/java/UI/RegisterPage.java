package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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

public class RegisterPage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#fff5f8")),
                        new Stop(1, Color.web("#fff9e6"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        root.setTop(createNavBar("BUPT-TA", "Registration page", false, stage));
        root.setBottom(createNavBar("BUPT-TA", "LI HUA | computer department", false, stage));

        HBox main = new HBox(25);
        main.setPadding(new Insets(25, 40, 25, 40));

        StackPane left = createLeftImageArea();
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setPrefWidth(770);

        VBox form = createForm(stage);
        HBox.setHgrow(form, Priority.ALWAYS);
        form.setPrefWidth(560);

        main.getChildren().addAll(left, form);
        root.setCenter(main);

        stage.setScene(new Scene(root, 1350, 820));
        stage.setTitle("BUPT-TA Registration Page");
        stage.show();
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

        Label subtitle = new Label("图片区占位，可替换为统一风格插画");
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

    private VBox createForm(Stage stage) {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);

        box.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ffd6e8")),
                        new Stop(1, Color.web("#ffcce6"))),
                new CornerRadii(20), Insets.EMPTY)));

        Label title = new Label("Sign up now");
        title.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.ITALIC, 28));
        title.setTextFill(Color.web("#ff66b3"));

        TextField username = createInput("#fff2cc", "Please enter your username");
        PasswordField pwd = createPassword("Please enter your password");
        PasswordField confirm = createPassword("Re-enter your password");

        Label roleLabel = new Label("Choose your role:");
        roleLabel.setFont(Font.font("Arial", 16));
        roleLabel.setTextFill(Color.BLACK);

        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("MO", "TA", "Teacher");
        role.setValue("MO");
        role.setPrefHeight(40);
        role.setPrefWidth(250);
        role.setStyle(
                "-fx-background-color: #ffb3d9;" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-font-size: 16px;" +
                        "-fx-text-fill: white;"
        );

        Button check = new Button("✓");
        check.setStyle(
                "-fx-background-color: #ff9933;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        check.setPrefSize(40, 40);

        HBox roleRow = new HBox(10, role, check);
        roleRow.setAlignment(Pos.CENTER_LEFT);

        VBox roleBox = new VBox(10, roleLabel, roleRow);
        roleBox.setAlignment(Pos.CENTER_LEFT);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Button register = new Button("Sign up");
        register.setPrefHeight(60);
        register.setMaxWidth(Double.MAX_VALUE);
        register.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffd699, #ffb3d9);" +
                        "-fx-background-radius: 30;" +
                        "-fx-text-fill: #ff66b3;" +
                        "-fx-font-family: 'Comic Sans MS';" +
                        "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );

        register.setOnAction(e -> {
            if (username.getText().trim().isEmpty()) {
                System.out.println("用户名不能为空！");
                return;
            }

            if (pwd.getText().trim().isEmpty() || confirm.getText().trim().isEmpty()) {
                System.out.println("密码不能为空！");
                return;
            }

            if (!pwd.getText().equals(confirm.getText())) {
                System.out.println("两次密码不一致！");
                return;
            }

            System.out.println("注册成功！当前角色：" + role.getValue());

            try {
                new LoginPage().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        box.getChildren().addAll(title, username, pwd, confirm, roleBox, register);
        return box;
    }

    private TextField createInput(String color, String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(50);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0 15 0 15;" +
                        "-fx-font-size: 16px;" +
                        "-fx-prompt-text-fill: #999999;"
        );
        return field;
    }

    private PasswordField createPassword(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(50);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle(
                "-fx-background-color: linear-gradient(to right, #ffcce6, #ffb3d9);" +
                        "-fx-background-radius: 25;" +
                        "-fx-border-radius: 25;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0 15 0 15;" +
                        "-fx-font-size: 16px;" +
                        "-fx-prompt-text-fill: #999999;"
        );
        return field;
    }

    private HBox createNavBar(String left, String right, boolean clickable, Stage stage) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ffe6e6")),
                        new Stop(1, Color.web("#ffd6d6"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        Label l = new Label(left);
        l.setTextFill(Color.BLACK);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label r = new Label(right);
        r.setTextFill(Color.web("#1f3b8f"));
        r.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Polygon red = new Polygon(0, 8, 8, 0, 16, 8, 8, 16);
        red.setFill(Color.web("#ff4d4d"));

        Circle purple = new Circle(9);
        purple.setFill(Color.web("#b266ff"));

        HBox rightBox = new HBox(10, r, red, purple);
        rightBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(l, spacer, rightBox);
        return bar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}