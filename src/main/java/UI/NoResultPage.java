package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class NoResultPage extends Application {

    // 这里用来模拟“搜索框输入的关键词”
    // 以后你做真正搜索功能时，把它改成从上一页传值
    public static String searchKeyword = "delicious";

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setBackground(UIStyleUtil.createPageBackground());

        root.setTop(UIStyleUtil.createNavBar("BUPT-TA", "LI HUA  |  computer department"));
        root.setLeft(createSideBar());
        root.setCenter(createMainContent());
        root.setBottom(UIStyleUtil.createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setTitle("BUPT-TA No Result");
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
        Button jobsBtn = UIStyleUtil.createMenuButton("More Jobs", true);
        Button resumeBtn = UIStyleUtil.createMenuButton("Resume\nDatabase", false);
        Button statusBtn = UIStyleUtil.createMenuButton("application\nstatus", false);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);

        sideBar.getChildren().addAll(dashBtn, jobsBtn, resumeBtn, statusBtn, filler);
        return sideBar;
    }

    private AnchorPane createMainContent() {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        StackPane searchBar = createSearchBar(searchKeyword);
        AnchorPane.setTopAnchor(searchBar, 28.0);
        AnchorPane.setLeftAnchor(searchBar, 40.0);
        AnchorPane.setRightAnchor(searchBar, 40.0);

        VBox centerBox = new VBox(18);
        centerBox.setAlignment(Pos.CENTER);

        Label resultLabel = new Label("Nothing found...");
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        resultLabel.setTextFill(Color.web("#4664a8"));

        Label hintLabel = new Label("No jobs matched \"" + searchKeyword + "\"");
        hintLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        hintLabel.setTextFill(Color.web("#ff66b3"));

        Label hintLabel2 = new Label("Try another course name, teacher name, or keyword.");
        hintLabel2.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        hintLabel2.setTextFill(Color.web("#888888"));

        VBox card = new VBox(18, resultLabel, hintLabel, hintLabel2);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setPrefWidth(620);
        card.setPrefHeight(260);
        card.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(28), Insets.EMPTY)
        ));
        card.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(28),
                new BorderWidths(2)
        )));

        centerBox.getChildren().add(card);

        AnchorPane.setTopAnchor(centerBox, 180.0);
        AnchorPane.setLeftAnchor(centerBox, 220.0);
        AnchorPane.setRightAnchor(centerBox, 220.0);

        Button backBtn = UIStyleUtil.createBackButton();
        AnchorPane.setBottomAnchor(backBtn, 28.0);
        AnchorPane.setLeftAnchor(backBtn, 45.0);

        main.getChildren().addAll(searchBar, centerBox, backBtn);
        return main;
    }

    private StackPane createSearchBar(String keyword) {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 22, 0, 22));
        bar.setPrefHeight(56);

        bar.setBackground(new Background(
                new BackgroundFill(Color.web("#ffd6e8"), new CornerRadii(28), Insets.EMPTY)
        ));

        Label searchIcon = new Label("⌕");
        searchIcon.setTextFill(Color.WHITE);
        searchIcon.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        Region line = new Region();
        line.setPrefWidth(3);
        line.setPrefHeight(32);
        line.setBackground(new Background(
                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Label text = new Label(keyword);
        text.setTextFill(Color.WHITE);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        bar.getChildren().addAll(searchIcon, line, text);
        return new StackPane(bar);
    }

    public static void main(String[] args) {
        launch(args);
    }
}