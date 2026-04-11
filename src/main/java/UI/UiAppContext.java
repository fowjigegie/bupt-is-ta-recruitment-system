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
    private String editingJobId;
    private String selectedApplicationId;
    private String selectedChatPeerUserId;

    private UiAppContext(StartupReport startupReport, AuthService authService, UiServices services) {
        this.startupReport = startupReport;
        this.authService = authService;
        this.sessionState = new SessionState();
        this.services = services;
    }

    public static UiAppContext createDefault() {
        // US00: 启动时先初始化数据目录，再装配 AuthService 和各业务 Service
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

    public String displayNameForUser(String userId) {
        return services.userRepository().findByUserId(userId)
            .map(UserAccount::displayName)
            .orElse(userId);
    }

    public String formatUserLabel(String userId) {
        String displayName = displayNameForUser(userId);
        if (displayName.equals(userId)) {
            return userId;
        }
        return displayName + " (" + userId + ")";
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

    /**
     * Editing context for MO Post Vacancies page.
     * Only set when the user clicks \"Edit Details\" from Job Management.
     */
    public void beginEditJob(String jobId) {
        editingJobId = blankToNull(jobId);
    }

    public String editingJobId() {
        return editingJobId;
    }

    public void clearJobEdit() {
        editingJobId = null;
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
        // US08: 从 Job Detail、Dashboard 等页面跳进 Messages 时，先把“当前岗位 + 当前聊天对象”记住，
        // 消息页就能直接定位到正确的 conversation，而不是让用户重新手动选择。
        selectJob(jobId);
        selectChatPeer(peerUserId);
    }

    public void clearSelections() {
        selectedJobId = null;
        editingJobId = null;
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
