package UI;

import com.bupt.tarecruitment.auth.UserRole;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;

public final class NavigationManager {
    private final Stage stage;
    private final UiAppContext context;
    private final Deque<PageId> history;

    private PageId currentPage;

    public NavigationManager(Stage stage, UiAppContext context) {
        this.stage = stage;
        this.context = context;
        this.history = new ArrayDeque<>();
    }

    public UiAppContext context() {
        return context;
    }

    public SessionState session() {
        return context.session();
    }

    public PageId currentPage() {
        return currentPage;
    }

    public void goTo(PageId pageId) {
        navigate(pageId, true, false);
    }

    public void replace(PageId pageId) {
        navigate(pageId, false, false);
    }

    public void resetTo(PageId pageId) {
        navigate(pageId, false, true);
    }

    public void goBack() {
        if (!history.isEmpty()) {
            PageId previousPage = history.pop();
            navigate(previousPage, false, false);
            return;
        }

        if (session().isAuthenticated()) {
            resetTo(roleHome(session().role()));
            return;
        }

        resetTo(PageId.LOGIN);
    }

    public void goToRoleHome(UserRole role) {
        resetTo(roleHome(role));
    }

    public void logout() {
        context.logout();
        resetTo(PageId.LOGIN);
    }

    private void navigate(PageId requestedPage, boolean pushHistory, boolean clearHistory) {
        if (clearHistory) {
            history.clear();
        }

        PageId targetPage = normalizeTarget(requestedPage);
        if (pushHistory && currentPage != null && currentPage != targetPage) {
            history.push(currentPage);
        }
        currentPage = targetPage;

        Scene scene = switch (targetPage) {
            case LOGIN -> LoginPage.createScene(this, context);
            case REGISTER -> RegisterPage.createScene(this, context);
            case APPLICANT_DASHBOARD -> DashboardPages.createScene(this, context);
            case MORE_JOBS -> MoreJobsPage.createScene(this, context);
            case RESUME_DATABASE -> ResumeDatabasePage.createScene(this, context);
            case JOB_DETAIL -> JobDetailPage.createScene(this, context);
            case MESSAGES -> MessagesPage.createScene(this, context);
            case INTERVIEW_INVITATION -> InterviewInvitationPage.createScene(this, context);
            case MO_DASHBOARD -> ModuleOrganizerDashboardPage.createScene(this, context);
            case POST_VACANCIES -> PostVacanciesPage.createScene(this, context);
            case JOB_MANAGEMENT -> JobManagementPage.createScene(this, context);
            case APPLICATION_REVIEW -> ApplicationReviewPage.createScene(this, context);
            case ADMIN_DASHBOARD -> AdminDashboardPage.createScene(this, context);
        };

        stage.setTitle(titleFor(targetPage));
        stage.setScene(scene);
        stage.show();
    }

    private PageId normalizeTarget(PageId requestedPage) {
        UserRole requiredRole = requiredRole(requestedPage);
        if (requiredRole == null) {
            return requestedPage;
        }

        if (!session().isAuthenticated()) {
            return PageId.LOGIN;
        }

        if (session().role() != requiredRole) {
            return roleHome(session().role());
        }

        return requestedPage;
    }

    private UserRole requiredRole(PageId pageId) {
        return switch (pageId) {
            case APPLICANT_DASHBOARD, MORE_JOBS, RESUME_DATABASE, JOB_DETAIL, MESSAGES, INTERVIEW_INVITATION ->
                UserRole.APPLICANT;
            case MO_DASHBOARD, POST_VACANCIES, JOB_MANAGEMENT, APPLICATION_REVIEW -> UserRole.MO;
            case ADMIN_DASHBOARD -> UserRole.ADMIN;
            default -> null;
        };
    }

    private PageId roleHome(UserRole role) {
        return switch (role) {
            case APPLICANT -> PageId.APPLICANT_DASHBOARD;
            case MO -> PageId.MO_DASHBOARD;
            case ADMIN -> PageId.ADMIN_DASHBOARD;
        };
    }

    private String titleFor(PageId pageId) {
        return switch (pageId) {
            case LOGIN -> "BUPT-TA Login";
            case REGISTER -> "BUPT-TA Register";
            case APPLICANT_DASHBOARD -> "BUPT-TA Applicant Dashboard";
            case MORE_JOBS -> "BUPT-TA More Jobs";
            case RESUME_DATABASE -> "BUPT-TA Resume Database";
            case JOB_DETAIL -> "BUPT-TA Job Detail";
            case MESSAGES -> "BUPT-TA Messages";
            case INTERVIEW_INVITATION -> "BUPT-TA Interview Invitation";
            case MO_DASHBOARD -> "BUPT-TA MO Dashboard";
            case POST_VACANCIES -> "BUPT-TA Post Vacancies";
            case JOB_MANAGEMENT -> "BUPT-TA Job Management";
            case APPLICATION_REVIEW -> "BUPT-TA Application Review";
            case ADMIN_DASHBOARD -> "BUPT-TA Admin Dashboard";
        };
    }
}
