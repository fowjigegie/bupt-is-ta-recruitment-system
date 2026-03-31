package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PostVacancyPage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setBackground(UIStyleUtil.createPageBackground());

        root.setTop(UIStyleUtil.createNavBar("BUPT-TA", "Ms Kina  |  computer department"));
        root.setLeft(createSideBar());
        root.setCenter(createMainContent());
        root.setBottom(UIStyleUtil.createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA Post Vacancy");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createSideBar() {
        VBox sideBar = new VBox();
        sideBar.setPrefWidth(180);
        sideBar.setBackground(new Background(
                new BackgroundFill(Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Button dashBtn = UIStyleUtil.createMenuButton("Dash Board", false);
        Button postBtn = UIStyleUtil.createMenuButton("Post\nVacancies", true);
        Button jobBtn = UIStyleUtil.createMenuButton("Job\nManagement", false);
        Button reviewBtn = UIStyleUtil.createMenuButton("Application\nreview", false);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, postBtn, jobBtn, reviewBtn, filler);
        return sideBar;
    }

    private AnchorPane createMainContent() {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        Label titleLabel = new Label("New Posting");
        titleLabel.setStyle(
                "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #ff66b3;"
        );

        VBox formBox = new VBox(18);
        formBox.setPadding(new Insets(28));
        formBox.setPrefWidth(930);
        formBox.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(26), Insets.EMPTY)
        ));
        formBox.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(26),
                new BorderWidths(2)
        )));

        HBox firstRow = new HBox(18,
                createLabeledField("Course Title", 390),
                createLabeledField("Taught By", 390)
        );

        VBox classesBox = createLabeledArea("Classes in Need of Assistance", 798, 80);
        VBox descBox = createLabeledArea("Job Description", 798, 130);
        VBox reqBox = createLabeledArea("Job Requirements", 798, 150);

        Button publishBtn = new Button("Publish");
        publishBtn.setPrefSize(180, 52);
        publishBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #fff0a8, #ffb3d9);" +
                "-fx-text-fill: #ff4fa3;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 26;" +
                "-fx-cursor: hand;"
        );

        HBox buttonRow = new HBox(publishBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        formBox.getChildren().addAll(
                firstRow,
                classesBox,
                descBox,
                reqBox,
                buttonRow
        );

        VBox content = new VBox(22, titleLabel, formBox);

        AnchorPane.setTopAnchor(content, 35.0);
        AnchorPane.setLeftAnchor(content, 40.0);
        AnchorPane.setRightAnchor(content, 40.0);

        Button backBtn = UIStyleUtil.createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(content, backBtn);
        return main;
    }

    private VBox createLabeledField(String labelText, double fieldWidth) {
        VBox box = new VBox(8);

        Label label = new Label(labelText + " :");
        label.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        TextField field = new TextField();
        field.setPrefWidth(fieldWidth);
        field.setPrefHeight(44);
        field.setStyle(
                "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #ffd6e8;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 16px;"
        );

        box.getChildren().addAll(label, field);
        return box;
    }

    private VBox createLabeledArea(String labelText, double width, double height) {
        VBox box = new VBox(8);

        Label label = new Label(labelText + " :");
        label.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #4664a8;"
        );

        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefSize(width, height);
        area.setStyle(
                "-fx-background-color: #fff3f7;" +
                "-fx-background-radius: 20;" +
                "-fx-border-color: #ffd6e8;" +
                "-fx-border-radius: 20;" +
                "-fx-border-width: 2;" +
                "-fx-font-size: 16px;"
        );

        box.getChildren().addAll(label, area);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}