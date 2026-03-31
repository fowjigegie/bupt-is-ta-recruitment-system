package UI;

import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;

public final class UiAppContext {
    private final StartupReport startupReport;
    private final AuthService authService;
    private final SessionState sessionState;
    private final UiServices services;

    private String selectedJobId;
    private String selectedApplicationId;
    private String selectedChatPeerUserId;

    private UiAppContext(StartupReport startupReport, AuthService authService, UiServices services) {
        this.startupReport = startupReport;
        this.authService = authService;
        this.sessionState = new SessionState();
        this.services = services;
    }

    public static UiAppContext createDefault() {
        StartupReport startupReport = new ProjectBootstrap().initialize();
        AuthService authService = new AuthService(
            new TextFileUserRepository(startupReport.dataDirectory()),
            new AuthValidator()
        );
        UiServices services = UiServices.create(startupReport.dataDirectory());
        return new UiAppContext(startupReport, authService, services);
    }

    public StartupReport startupReport() {
        return startupReport;
    }

    public AuthService authService() {
        return authService;
    }

    public SessionState session() {
        return sessionState;
    }

    public UiServices services() {
        return services;
    }

    public void signIn(UserAccount account) {
        sessionState.setCurrentUser(account);
    }

    public void logout() {
        authService.logout();
        sessionState.clear();
        clearSelections();
    }

    public void selectJob(String jobId) {
        selectedJobId = blankToNull(jobId);
    }

    public String selectedJobId() {
        return selectedJobId;
    }

    public void selectApplication(String applicationId) {
        selectedApplicationId = blankToNull(applicationId);
    }

    public String selectedApplicationId() {
        return selectedApplicationId;
    }

    public void selectChatPeer(String peerUserId) {
        selectedChatPeerUserId = blankToNull(peerUserId);
    }

    public String selectedChatPeerUserId() {
        return selectedChatPeerUserId;
    }

    public void openChatContext(String jobId, String peerUserId) {
        selectJob(jobId);
        selectChatPeer(peerUserId);
    }

    public void clearSelections() {
        selectedJobId = null;
        selectedApplicationId = null;
        selectedChatPeerUserId = null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
