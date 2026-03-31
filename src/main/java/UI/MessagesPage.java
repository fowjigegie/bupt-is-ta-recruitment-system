package UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MessagesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MESSAGES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(18);
        center.setPadding(new Insets(25, 40, 25, 40));

        VBox listBox = new VBox(18,
            createMessageRow("Kina", "Re: You need to check your e-mails regularly...", true, 2, false),
            createMessageRow("Naby", "Re: You have to complete your personal info...", false, -1, true),
            createMessageRow("Cindy", "Sent: I am going to the supermarket.", false, -1, false),
            createMessageRow("Baron (admin)", "Sent: Thank you very much.", false, -1, false),
            createMessageRow("Mat", "Re: It seems that you are not suitable for this...", false, -1, false)
        );

        HBox footer = new HBox(UiTheme.createBackButton(nav));
        footer.setAlignment(Pos.CENTER_LEFT);

        center.getChildren().addAll(
            UiTheme.createPageHeading("Messages"),
            buildChatContextHint(context),
            UiTheme.createMutedText("Message list is still static in this step. Next step will render real conversation data."),
            listBox,
            footer
        );

        BorderPane root = UiTheme.createPage(
            "Messages",
            UiTheme.createApplicantSidebar(nav, null),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    private static Label buildChatContextHint(UiAppContext context) {
        String selectedJobId = context.selectedJobId();
        String peerUserId = context.selectedChatPeerUserId();

        String text;
        if (selectedJobId == null || peerUserId == null) {
            text = "No active chat context yet. Open chat from a job card/detail page.";
        } else {
            text = "Active chat context: jobId=%s, peer=%s".formatted(selectedJobId, peerUserId);
        }
        return UiTheme.createMutedText(text);
    }

    private static HBox createMessageRow(String name, String preview, boolean unreadCountVisible, int unreadCount, boolean redDot) {
        HBox outer = new HBox();
        outer.setAlignment(Pos.CENTER_LEFT);

        StackPane wrapper = new StackPane();
        wrapper.setPrefHeight(92);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        Region cardBg = new Region();
        cardBg.setPrefHeight(92);
        cardBg.setMaxWidth(Double.MAX_VALUE);
        cardBg.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(24), Insets.EMPTY)));
        cardBg.setBorder(new Border(new BorderStroke(
            Color.web("#f4d9e6"),
            BorderStrokeStyle.SOLID,
            new CornerRadii(24),
            new BorderWidths(1.5)
        )));

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 22, 0, 22));

        StackPane avatar = createAvatar();

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.web("#333333"));
        nameLabel.setMinWidth(145);

        Region divider = new Region();
        divider.setPrefWidth(3);
        divider.setPrefHeight(46);
        divider.setBackground(new Background(new BackgroundFill(Color.web("#ffe6f0"), CornerRadii.EMPTY, Insets.EMPTY)));

        Label previewLabel = new Label(preview);
        previewLabel.setFont(Font.font("Arial", 18));
        previewLabel.setTextFill(Color.web("#9a8aa0"));

        row.getChildren().addAll(avatar, nameLabel, divider, previewLabel);

        wrapper.getChildren().addAll(cardBg, row);
        StackPane.setAlignment(row, Pos.CENTER_LEFT);

        if (unreadCountVisible && unreadCount >= 0) {
            Circle badgeCircle = new Circle(18, Color.web("#ff4d4d"));
            Label badgeLabel = new Label(String.valueOf(unreadCount));
            badgeLabel.setTextFill(Color.WHITE);
            badgeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

            StackPane badge = new StackPane(badgeCircle, badgeLabel);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            badge.setTranslateX(-18);
            badge.setTranslateY(8);
            wrapper.getChildren().add(badge);
        } else if (redDot) {
            Circle dot = new Circle(10, Color.web("#ff4d4d"));
            StackPane.setAlignment(dot, Pos.TOP_RIGHT);
            dot.setTranslateX(-22);
            dot.setTranslateY(12);
            wrapper.getChildren().add(dot);
        }

        HBox.setHgrow(wrapper, Priority.ALWAYS);
        outer.getChildren().add(wrapper);
        return outer;
    }

    private static StackPane createAvatar() {
        Circle outerCircle = new Circle(28);
        outerCircle.setFill(Color.web("#ffd6e8"));
        return new StackPane(outerCircle);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
