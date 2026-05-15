package com.bupt.tarecruitment.ui;

import javafx.stage.Stage;

/**
 * 统一封装 JavaFX 页面启动入口。
 */
final class UiLauncher {
    private UiLauncher() {
    }

    static void launch(PageId pageId, Stage stage) {
        NavigationManager navigationManager = new NavigationManager(stage, UiAppContext.createDefault());
        navigationManager.replace(pageId);
    }
}
