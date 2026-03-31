package UI;

import javafx.stage.Stage;

final class UiLauncher {
    private UiLauncher() {
    }

    static void launch(PageId pageId, Stage stage) {
        NavigationManager navigationManager = new NavigationManager(stage, UiAppContext.createDefault());
        navigationManager.replace(pageId);
    }
}
