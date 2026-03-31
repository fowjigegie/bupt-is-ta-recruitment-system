package UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class UIStyleUtil {

    public static Background createPageBackground() {
        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#fff5f8")),
                new Stop(1, Color.web("#fff9e6"))
        };
        LinearGradient pageBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops
        );
        return new Background(new BackgroundFill(pageBg, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static HBox createNavBar(String leftText, String rightText) {
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

        navBar.setBackground(new Background(
                new BackgroundFill(navGradient, CornerRadii.EMPTY, Insets.EMPTY)
        ));

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

    public static Button createMenuButton(String text, boolean selected) {
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

    public static Button createBackButton() {
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

    public static Region createBottomBar() {
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

    public static HBox createPager(int totalPages, int activePage) {
        HBox pager = new HBox(0);
        pager.setAlignment(Pos.CENTER);

        for (int i = 1; i <= totalPages; i++) {
            boolean active = i == activePage;
            boolean last = i == totalPages;
            pager.getChildren().add(createPageItem(String.valueOf(i), active, last));
        }

        return pager;
    }

    private static StackPane createPageItem(String text, boolean active, boolean last) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        StackPane item = new StackPane(label);
        item.setPrefSize(48, 36);

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
}