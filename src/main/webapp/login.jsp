<%@ page import="com.bupt.tarecruitment.auth.AuthService" %>
<%@ page import="com.bupt.tarecruitment.auth.AuthValidator" %>
<%@ page import="com.bupt.tarecruitment.auth.TextFileUserRepository" %>
<%@ page import="com.bupt.tarecruitment.auth.UserAccount" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%@ page import="java.nio.file.Paths" %>
<%@ page import="java.io.IOException" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private Path findDataDirectory(javax.servlet.ServletContext application) throws IOException {
        String configured = application.getInitParameter("taDataDirectory");
        if (configured != null && !configured.isBlank()) {
            Path configuredPath = Paths.get(configured);
            if (Files.exists(configuredPath.resolve("users.txt"))) {
                return configuredPath;
            }
        }

        Path workingDirCandidate = Paths.get(System.getProperty("user.dir")).resolve("data");
        if (Files.exists(workingDirCandidate.resolve("users.txt"))) {
            return workingDirCandidate;
        }

        String realRoot = application.getRealPath("/");
        if (realRoot != null) {
            Path current = Paths.get(realRoot).toAbsolutePath();
            for (int i = 0; i < 8 && current != null; i++) {
                Path candidate = current.resolve("data");
                if (Files.exists(candidate.resolve("users.txt"))) {
                    return candidate;
                }
                current = current.getParent();
            }
        }

        return workingDirCandidate;
    }
%>
<%
    if (session.getAttribute("currentUserId") != null) {
        response.sendRedirect("dashboard.jsp");
        return;
    }

    String error = request.getParameter("error");
    String success = request.getParameter("success");
    String userIdValue = request.getParameter("userId") == null ? "" : request.getParameter("userId");

    if ("POST".equalsIgnoreCase(request.getMethod())) {
        userIdValue = request.getParameter("userId") == null ? "" : request.getParameter("userId").trim();
        String passwordValue = request.getParameter("password") == null ? "" : request.getParameter("password");

        try {
            Path dataDirectory = findDataDirectory(application);
            AuthService service = new AuthService(new TextFileUserRepository(dataDirectory), new AuthValidator());
            UserAccount account = service.login(userIdValue, passwordValue);

            session.setAttribute("currentUserId", account.userId());
            session.setAttribute("currentDisplayName", account.displayName());
            session.setAttribute("currentUserRole", account.role().name());
            session.setAttribute("currentUserStatus", account.status().name());

            response.sendRedirect("dashboard.jsp");
            return;
        } catch (Exception exception) {
            error = exception.getMessage();
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login | TA Recruitment</title>
    <link rel="stylesheet" href="assets/auth.css">
</head>
<body class="auth-body">
<div class="shell">
    <header class="topbar">
        <a class="brand" href="index.jsp">TA Recruitment</a>
        <nav class="nav">
            <a href="index.jsp">Home</a>
            <a class="is-active" href="login.jsp">Login</a>
            <a href="register.jsp">Register</a>
        </nav>
    </header>

    <main class="auth-layout">
        <section class="auth-panel">
            <p class="auth-label">Account Access</p>
            <h1>Login to continue your TA recruitment journey</h1>
            <p class="auth-summary">
                Sign in with your project user ID and password. The page keeps the same role-aware account rules
                as the existing auth module, so applicant, organiser, and admin accounts can share one entry point.
            </p>

            <% if (error != null && !error.isBlank()) { %>
            <div class="message message-error"><%= error %></div>
            <% } %>
            <% if (success != null && !success.isBlank()) { %>
            <div class="message message-success"><%= success %></div>
            <% } %>

            <form class="auth-form" action="login.jsp" method="post">
                <div class="field">
                    <label for="userId">User ID</label>
                    <input
                        id="userId"
                        name="userId"
                        type="text"
                        placeholder="e.g. ta001 / mo001 / admin001"
                        value="<%= userIdValue %>"
                        required
                    >
                    <div class="field-hint">Applicants use <code>ta...</code>, organisers use <code>mo...</code>, admins use <code>admin...</code>.</div>
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" placeholder="Enter your password" required>
                </div>

                <div class="auth-meta">
                    <span>Plain Java auth demo adapted for JSP presentation.</span>
                    <a href="register.jsp">Need an account?</a>
                </div>

                <div class="auth-submit">
                    <button class="btn btn-primary" type="submit">Login</button>
                </div>
            </form>

            <p class="auth-foot">
                Planned backend mapping: submit to the existing auth service and route users into applicant,
                organiser, or admin features after successful login.
            </p>
        </section>

        <aside class="side-stack">
            <section class="side-card">
                <h3>Demo Accounts</h3>
                <ul>
                    <li>Applicant: <strong>ta001</strong> / <strong>demo-ta-password</strong></li>
                    <li>Module organiser: <strong>mo001</strong> / <strong>demo-mo-password</strong></li>
                    <li>Admin: <strong>admin001</strong> / <strong>demo-admin-password</strong></li>
                </ul>
            </section>

            <section class="side-card">
                <h3>Login Notes</h3>
                <div class="chip-row">
                    <span class="chip">Role aware</span>
                    <span class="chip">JSP view layer</span>
                    <span class="chip">Text-file storage</span>
                </div>
                <p>
                    This page is intentionally shaped around the existing project contracts, so the form field names
                    already match the backend auth workflow: <code>userId</code> and <code>password</code>.
                </p>
            </section>
        </aside>
    </main>
</div>
</body>
</html>
