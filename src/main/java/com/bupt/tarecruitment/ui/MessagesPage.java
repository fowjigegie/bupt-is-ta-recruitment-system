package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.auth.UserRole;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * 消息页，负责页面入口和发送消息动作。
 */
public class MessagesPage extends Application {
    @Override
    public void start(Stage stage) {
        UiLauncher.launch(PageId.MESSAGES, stage);
    }

    static Scene createScene(NavigationManager nav, UiAppContext context) {
        VBox center = new VBox(16);
        center.setPadding(new Insets(24, 36, 24, 36));

        if (!context.session().isAuthenticated()) {
            center.getChildren().addAll(
                UiTheme.createPageHeading("Messages"),
                UiTheme.createWhiteCard("Sign in required", "Please sign in before opening messages."),
                new HBox(UiTheme.createBackButton(nav))
            );
            BorderPane root = UiTheme.createPage("Messages", null, center, nav, context);
            return UiTheme.createScene(root);
        }

        String userId = context.session().userId();
        MessagesWorkspace workspace = MessagesWorkspace.create();

        Runnable refreshThreads = () -> {
            List<ChatThread> threads = MessagesConversationView.buildThreads(context, userId);
            MessagesConversationView.renderThreadList(context, threads, workspace);
        };

        Runnable sendMessageAction = () -> {
            ChatThread active = workspace.selectedThread();
            if (active == null) {
                workspace.showError("Please select a conversation first.");
                return;
            }

            String content = workspace.messageInput().getText().trim();
            if (content.isBlank()) {
                workspace.showError("Message content must not be blank.");
                return;
            }

            try {
                context.services().messageService().sendMessage(
                    active.jobId(),
                    userId,
                    active.peerUserId(),
                    content
                );
                workspace.messageInput().clear();
                workspace.showSuccess("Message sent.");
                refreshThreads.run();
                workspace.setSelectedThread(active);
                MessagesConversationView.renderConversation(context, userId, active, workspace);
            } catch (IllegalArgumentException exception) {
                workspace.showError(exception.getMessage());
            }
        };

        workspace.refreshButton().setOnAction(event -> refreshThreads.run());
        workspace.sendButton().setOnAction(event -> sendMessageAction.run());
        workspace.messageInput().setOnAction(event -> sendMessageAction.run());

        HBox contentRow = new HBox(14, workspace.leftPane(), workspace.rightPane());

        center.getChildren().addAll(
            UiTheme.createPageHeading("Messages"),
            UiTheme.createMutedText("Chat with MO per job context. Select a conversation and send messages below."),
            contentRow
        );
        if (context.session().role() == UserRole.MO) {
            HBox footer = new HBox(UiTheme.createBackButton(nav));
            footer.setAlignment(Pos.CENTER_LEFT);
            center.getChildren().add(footer);
        }

        refreshThreads.run();

        BorderPane root = UiTheme.createPage(
            "Messages",
            context.session().role() == UserRole.MO
                ? UiTheme.createMoSidebar(nav, null)
                : UiTheme.createApplicantSidebar(nav, null),
            center,
            nav,
            context
        );
        return UiTheme.createScene(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
