package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.auth.UserRole;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 统一管理 JavaFX 页面之间的跳转与替换。
 */
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
        // 普通页面跳转：会记录历史栈
        navigate(pageId, true, false);
    }

    public void replace(PageId pageId) {
        // 页面替换：不记录历史栈（常用于刷新当前页）
        navigate(pageId, false, false);
    }

    public void resetTo(PageId pageId) {
        // 重置为某页，并清空历史栈（例如登录后跳转到主页）
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

        // US00: 统一做权限/登录拦截，未登录或角色不匹配会被重定向
        PageId targetPage = normalizeTarget(requestedPage);

        // 只有实际发生跳转时才压栈
        if (pushHistory && currentPage != null && currentPage != targetPage) {
            history.push(currentPage);
        }

        currentPage = targetPage;

        Scene scene = switch (targetPage) {
            case LOGIN -> LoginPage.createScene(this, context);
            case REGISTER -> RegisterPage.createScene(this, context);
            case APPLICANT_DASHBOARD -> DashboardPages.createScene(this, context);
            case TA_WORKLOAD -> TaWorkloadPage.createScene(this, context);
            case MORE_JOBS -> MoreJobsPage.createScene(this, context);
            case RESUME_DATABASE -> ResumeDatabasePage.createScene(this, context);
            case SKILL_SELECTOR -> SkillSelectionPage.createScene(this, context);
            case JOB_DETAIL -> JobDetailPage.createScene(this, context);
            case MESSAGES -> MessagesPage.createScene(this, context);
            case INTERVIEW_INVITATION -> InterviewInvitationPage.createScene(this, context);
            case MO_DASHBOARD -> ModuleOrganizerDashboardPage.createScene(this, context);
            case POST_VACANCIES -> PostVacanciesPage.createScene(this, context);
            case JOB_MANAGEMENT -> JobManagementPage.createScene(this, context);
            case APPLICATION_REVIEW -> ApplicationReviewPage.createScene(this, context);
            case MO_INSIGHTS -> MoInsightsPage.createScene(this, context);
            case ADMIN_DASHBOARD -> AdminDashboardPage.createScene(this, context);
            case ADMIN_ANALYTICS -> AdminAnalyticsPage.createScene(this, context);
        };

        // 记录当前窗口状态，避免页面切换后窗口大小/位置突变
        boolean restoreWindowState = stage.getScene() != null;
        double currentWidth = stage.getWidth();
        double currentHeight = stage.getHeight();
        double currentX = stage.getX();
        double currentY = stage.getY();
        boolean maximized = stage.isMaximized();
        boolean fullScreen = stage.isFullScreen();

        stage.setTitle(titleFor(targetPage));
        stage.setScene(scene);
        stage.show();

        if (restoreWindowState) {
            // 恢复窗口大小/位置/最大化状态
            if (!Double.isNaN(currentWidth) && currentWidth > 0) {
                stage.setWidth(currentWidth);
            }
            if (!Double.isNaN(currentHeight) && currentHeight > 0) {
                stage.setHeight(currentHeight);
            }
            if (!Double.isNaN(currentX)) {
                stage.setX(currentX);
            }
            if (!Double.isNaN(currentY)) {
                stage.setY(currentY);
            }
            if (maximized) {
                stage.setMaximized(true);
            }
            if (fullScreen) {
                stage.setFullScreen(true);
            }
        }
    }

    private PageId normalizeTarget(PageId requestedPage) {
        if (requestedPage == PageId.MESSAGES) {
            if (!session().isAuthenticated()) {
                return PageId.LOGIN;
            }

            if (session().role() == UserRole.APPLICANT || session().role() == UserRole.MO) {
                return requestedPage;
            }

            return roleHome(session().role());
        }

        UserRole requiredRole = requiredRole(requestedPage);
        if (requiredRole == null) {
            return requestedPage;
        }

        // 没有登录则回到登录页
        if (!session().isAuthenticated()) {
            return PageId.LOGIN;
        }

        // 角色不匹配则跳回对应角色首页
        if (session().role() != requiredRole) {
            return roleHome(session().role());
        }

        return requestedPage;
    }

    private UserRole requiredRole(PageId pageId) {
        // 页面与角色的映射规则：用于统一权限拦截
        return switch (pageId) {
            case APPLICANT_DASHBOARD, TA_WORKLOAD, MORE_JOBS, RESUME_DATABASE, SKILL_SELECTOR, JOB_DETAIL, INTERVIEW_INVITATION ->
                UserRole.APPLICANT;
            case MO_DASHBOARD, POST_VACANCIES, JOB_MANAGEMENT, APPLICATION_REVIEW, MO_INSIGHTS -> UserRole.MO;
            case ADMIN_DASHBOARD, ADMIN_ANALYTICS -> UserRole.ADMIN;
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
            case TA_WORKLOAD -> "BUPT-TA TA Workload";
            case MORE_JOBS -> "BUPT-TA More Jobs";
            case RESUME_DATABASE -> "BUPT-TA Resume Database";
            case SKILL_SELECTOR -> "BUPT-TA Skill Selection";
            case JOB_DETAIL -> "BUPT-TA Job Detail";
            case MESSAGES -> "BUPT-TA Messages";
            case INTERVIEW_INVITATION -> "BUPT-TA Application Status";
            case MO_DASHBOARD -> "BUPT-TA MO Dashboard";
            case POST_VACANCIES -> "BUPT-TA Post Vacancies";
            case JOB_MANAGEMENT -> "BUPT-TA Job Management";
            case APPLICATION_REVIEW -> "BUPT-TA Application Review";
            case MO_INSIGHTS -> "BUPT-TA MO Insights";
            case ADMIN_DASHBOARD -> "BUPT-TA Admin Dashboard";
            case ADMIN_ANALYTICS -> "BUPT-TA Admin Data Analytics";
        };
    }
}
