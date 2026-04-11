package UI;

import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class RegisterPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.REGISTER, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        BorderPane root = new BorderPane();
        root.setBackground(UiTheme.pageBackground());
        root.setTop(UiTheme.createLandingNavBar("BUPT-TA", "Back to login", () -> nav.goTo(PageId.LOGIN)));
        root.setBottom(UiTheme.createLandingNavBar("BUPT-TA", "Use role-aligned account IDs", null));

        HBox main = new HBox(25);
        main.setPadding(new Insets(25, 40, 25, 40));

        var left = UiTheme.createIllustrationCard(
            "Registration",
            "",
            "/UI/assets/login/login.png"
        );
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setPrefWidth(770);

        VBox form = createForm(nav, context);
        HBox.setHgrow(form, Priority.ALWAYS);
        form.setPrefWidth(560);

        main.getChildren().addAll(left, form);
        root.setCenter(main);
        return UiTheme.createScene(root);
    }

    private static VBox createForm(NavigationManager nav, UiAppContext context) {
        Label title = new Label("Sign up now");
        title.setStyle(
            "-fx-font-family: 'Comic Sans MS';" +
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        TextField usernameField = createInput("#fff2cc", "Please enter your username");
        PasswordField passwordField = createPassword("Please enter your password");
        PasswordField confirmField = createPassword("Re-enter your password");

        Label roleLabel = new Label("Choose your role:");
        roleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: black;");

        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.APPLICANT, UserRole.MO, UserRole.ADMIN);
        roleBox.setValue(UserRole.APPLICANT);
        roleBox.setPrefHeight(40);
        roleBox.setPrefWidth(260);
        roleBox.setStyle(
            "-fx-background-color: #ffb3d9;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-font-size: 16px;"
        );

        Label ruleHint = UiTheme.createMutedText(
            "Use ta### for APPLICANT, mo### for MO, and admin### for ADMIN."
        );
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        var registerButton = UiTheme.createPrimaryButton("Sign up", 420, 60);
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(event -> handleRegister(
            nav,
            context,
            usernameField,
            passwordField,
            confirmField,
            roleBox,
            statusLabel
        ));

        HBox roleRow = new HBox(10, roleBox);
        roleRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = UiTheme.createFormCard(
            title,
            usernameField,
            passwordField,
            confirmField,
            new VBox(10, roleLabel, roleRow),
            ruleHint,
            statusLabel,
            registerButton
        );

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private static void handleRegister(
        NavigationManager nav,
        UiAppContext context,
        TextField usernameField,
        PasswordField passwordField,
        PasswordField confirmField,
        ComboBox<UserRole> roleBox,
        Label statusLabel
    ) {
        statusLabel.setText("");

        // US00: 注册时先做两次密码一致性检查
        if (!passwordField.getText().equals(confirmField.getText())) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        try {
            // 1) 注册账号
            context.authService().register(
                usernameField.getText().trim(),
                passwordField.getText(),
                roleBox.getValue()
            );
            // 2) 注册成功后直接登录并进入对应角色首页
            // 这样用户不需要再返回登录页手动登录一次
            UserAccount account = context.authService().login(
                usernameField.getText().trim(),
                passwordField.getText()
            );
            context.signIn(account);
            nav.goToRoleHome(account.role());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static TextField createInput(String color, String prompt) {
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

    private static PasswordField createPassword(String prompt) {
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

    public static void main(String[] args) {
        launch(args);
    }
}
