package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ApplicationControlPage extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#fff5f8")),
                new Stop(1, Color.web("#fff9e6"))
        };
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE, bgStops),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        root.setTop(createNavBar());
        root.setLeft(createSideBar());
        root.setCenter(createMainContent());
        root.setBottom(createBottomBar());

        Scene scene = new Scene(root, 1350, 820);
        stage.setScene(scene);
        stage.setTitle("Application Control");
        stage.show();
    }

    // ================= 顶部 =================
    private HBox createNavBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 12, 24));

        bar.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,1,0,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ffe6e6")),
                        new Stop(1, Color.web("#ffd6d6"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        Label left = new Label("BUPT-TA");
        left.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label right = new Label("Baron  |  Administrator");
        right.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        right.setTextFill(Color.web("#1f3b8f"));

        Polygon red = new Polygon(0,8, 8,0, 16,8, 8,16);
        red.setFill(Color.web("#ff4d4d"));

        Circle purple = new Circle(9, Color.web("#b266ff"));

        HBox rightBox = new HBox(10, right, red, purple);
        rightBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(left, spacer, rightBox);
        return bar;
    }

    // ================= 左侧 =================
    private VBox createSideBar() {
        VBox side = new VBox();
        side.setPrefWidth(180);
        side.setBackground(new Background(new BackgroundFill(
                Color.web("#f5b4e6"), CornerRadii.EMPTY, Insets.EMPTY
        )));

        side.getChildren().addAll(
                createMenu("Dash Board", false),
                createMenu("TA Workload", false),
                createMenu("Application\nControl", true),
                new Region()
        );

        VBox.setVgrow(side.getChildren().get(3), Priority.ALWAYS);
        return side;
    }

    private Button createMenu(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setPrefWidth(180);
        btn.setMinHeight(70);
        btn.setWrapText(true);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        if (selected) {
            btn.setStyle("-fx-background-color:#4565a8; -fx-text-fill:white; -fx-background-radius:0 35 35 0;");
        } else {
            btn.setStyle("-fx-background-color:#f5b4e6; -fx-text-fill:white; -fx-border-color:white; -fx-border-width:0 0 2 0;");
        }
        return btn;
    }

    // ================= 主内容 =================
    private AnchorPane createMainContent() {
        AnchorPane main = new AnchorPane();
        main.setPadding(new Insets(25));

        VBox table = createTable();
        AnchorPane.setTopAnchor(table, 40.0);
        AnchorPane.setLeftAnchor(table, 40.0);
        AnchorPane.setRightAnchor(table, 40.0);

        Label end = new Label("No further information ...");
        end.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        end.setTextFill(Color.web("#4664a8"));
        AnchorPane.setTopAnchor(end, 520.0);
        AnchorPane.setLeftAnchor(end, 450.0);

        Button back = createBackBtn();
        AnchorPane.setBottomAnchor(back, 28.0);
        AnchorPane.setLeftAnchor(back, 45.0);

        main.getChildren().addAll(table, end, back);
        return main;
    }

    // ================= 表格 =================
    private VBox createTable() {
        VBox box = new VBox();
        box.setPadding(new Insets(22,28,22,28));

        box.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(24), Insets.EMPTY
        )));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#f4d9e6"), BorderStrokeStyle.SOLID,
                new CornerRadii(24), new BorderWidths(2)
        )));

        VBox rows = new VBox(0);
        rows.getChildren().addAll(
                createRow("Wang Wei", "CBUS201 ML TA\nEBUS204 OS TA", "Need to operate", true),
                createRow("Liu Min", "CBUS201 ML TA\nEBUS471 Embedded TA", "Done", false),
                createRow("Wu Yue", "EBUS212 Netw TA\nEBUS471 Embedded TA", "Done", false),
                createRow("Huang Meng", "CBUS201 ML TA\nCBUS201 ML TA", "Done", false)
        );

        box.getChildren().add(rows);
        return box;
    }

    // ================= 行（重点：可展开） =================
    private VBox createRow(String name, String jobs, String status, boolean hasDropdown) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16,10,16,10));

        Label nameL = new Label(name);
        nameL.setPrefWidth(180);
        nameL.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        nameL.setTextFill(Color.web("#4664a8"));

        Label jobL = new Label(jobs);
        jobL.setPrefWidth(340);
        jobL.setWrapText(true);
        jobL.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        jobL.setTextFill(hasDropdown ? Color.web("#ff5c8a") : Color.web("#96d57f"));

        Label statusL = new Label(status);
        statusL.setPrefWidth(180);
        statusL.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        statusL.setTextFill(status.equals("Done") ? Color.web("#96d57f") : Color.web("#ff5c8a"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detail = pinkBtn("View Details",130);

        VBox wrapper = new VBox(8);

        if (hasDropdown) {
            Button delete = pinkBtn("Delete ▼",100);

            VBox menu = new VBox(10,
                    new Label("CBUS201 ML TA"),
                    new Label("EBUS204 OS TA")
            );
            menu.setPadding(new Insets(12));
            menu.setBackground(new Background(new BackgroundFill(
                    Color.web("#ffe6f2"), new CornerRadii(18), Insets.EMPTY
            )));
            menu.setVisible(false);
            menu.setManaged(false);

            delete.setOnAction(e -> {
                boolean show = !menu.isVisible();
                menu.setVisible(show);
                menu.setManaged(show);
                delete.setText(show ? "Delete ▲" : "Delete ▼");
            });

            HBox top = new HBox(10, detail, delete);
            VBox right = new VBox(8, top, menu);

            row.getChildren().addAll(nameL, jobL, statusL, spacer, right);
        } else {
            row.getChildren().addAll(nameL, jobL, statusL, spacer, detail);
        }

        Region line = new Region();
        line.setPrefHeight(2);
        line.setBackground(new Background(new BackgroundFill(
                Color.web("#ffd6e8"), CornerRadii.EMPTY, Insets.EMPTY
        )));

        wrapper.getChildren().addAll(row, line);
        return wrapper;
    }

    // ================= 按钮 =================
    private Button pinkBtn(String text, double w){
        Button b = new Button(text);
        b.setPrefSize(w,38);
        b.setStyle(
                "-fx-background-color:#ffd6e8;" +
                "-fx-background-radius:19;" +
                "-fx-font-weight:bold;"
        );
        return b;
    }

    private Button createBackBtn(){
        Button b = new Button("⬅");
        b.setPrefSize(56,56);
        b.setStyle(
                "-fx-background-color:#b266ff;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:22;" +
                "-fx-background-radius:28;"
        );
        return b;
    }

    // ================= 底部 =================
    private Region createBottomBar() {
        Region bottom = new Region();
        bottom.setPrefHeight(18);
        bottom.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,1,0,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ffe6e6")),
                        new Stop(1, Color.web("#ffd6d6"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));
        return bottom;
    }

    public static void main(String[] args) {
        launch(args);
    }
}