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

public class LoginPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.LOGIN, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        BorderPane root = new BorderPane();
        root.setBackground(UiTheme.pageBackground());
        root.setTop(UiTheme.createLandingNavBar("BUPT-TA", "Registration page", () -> nav.goTo(PageId.REGISTER)));
        root.setBottom(UiTheme.createLandingNavBar("BUPT-TA", "JavaFX role-based shell", null));

        HBox mainContent = new HBox(25);
        mainContent.setPadding(new Insets(25, 40, 25, 40));

        var leftImageArea = UiTheme.createIllustrationCard(
            "BUPT-TA Recruitment",
            "Single-window navigation is now handled by a shared NavigationManager."
        );
        HBox.setHgrow(leftImageArea, Priority.ALWAYS);
        leftImageArea.setPrefWidth(770);

        VBox formArea = createRightFormArea(nav, context);
        HBox.setHgrow(formArea, Priority.ALWAYS);
        formArea.setPrefWidth(560);

        mainContent.getChildren().addAll(leftImageArea, formArea);
        root.setCenter(mainContent);

        return UiTheme.createScene(root);
    }

    private static VBox createRightFormArea(NavigationManager nav, UiAppContext context) {
        Label welcomeLabel = new Label("Welcome to log in");
        welcomeLabel.setStyle(
            "-fx-font-family: 'Comic Sans MS';" +
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        TextField usernameField = createField("Please enter your username");
        PasswordField passwordField = createPasswordField("Please enter your password");

        Label roleLabel = new Label("Choose your role:");
        roleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: black;");

        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.APPLICANT, UserRole.MO, UserRole.ADMIN);
        roleBox.setValue(UserRole.APPLICANT);
        roleBox.setPrefWidth(260);
        roleBox.setPrefHeight(42);
        roleBox.setStyle(
            "-fx-background-color: #ffb3d9;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-font-size: 16px;"
        );

        Label hintLabel = UiTheme.createMutedText(
            "Demo accounts: ta001 / demo-ta-password, mo001 / demo-mo-password, admin001 / demo-admin-password"
        );
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#b00020"));

        var loginButton = UiTheme.createPrimaryButton("Log in", 420, 60);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> handleLogin(nav, context, usernameField, passwordField, roleBox, statusLabel));

        Label normalText = new Label("Don't have an account yet? ");
        normalText.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");

        var signUpLink = UiTheme.createInlineLink("Please sign up");
        signUpLink.setOnAction(event -> nav.goTo(PageId.REGISTER));

        HBox signUpRow = new HBox(normalText, signUpLink);
        signUpRow.setAlignment(Pos.CENTER);

        VBox roleSection = new VBox(10, roleLabel, roleBox);
        roleSection.setAlignment(Pos.CENTER_LEFT);

        VBox card = UiTheme.createFormCard(
            welcomeLabel,
            usernameField,
            passwordField,
            roleSection,
            hintLabel,
            statusLabel,
            loginButton,
            signUpRow
        );

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private static void handleLogin(
        NavigationManager nav,
        UiAppContext context,
        TextField usernameField,
        PasswordField passwordField,
        ComboBox<UserRole> roleBox,
        Label statusLabel
    ) {
        statusLabel.setText("");

        try {
            UserAccount account = context.authService().login(
                usernameField.getText().trim(),
                passwordField.getText()
            );

            if (account.role() != roleBox.getValue()) {
                context.authService().logout();
                throw new IllegalArgumentException("Selected role does not match this account.");
            }

            context.signIn(account);
            nav.goToRoleHome(account.role());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(50);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle(
            "-fx-background-color: #fff2cc;" +
                "-fx-background-radius: 25;" +
                "-fx-border-radius: 25;" +
                "-fx-border-color: transparent;" +
                "-fx-padding: 0 15 0 15;" +
                "-fx-font-size: 16px;" +
                "-fx-prompt-text-fill: #999999;"
        );
        return field;
    }

    private static PasswordField createPasswordField(String prompt) {
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
