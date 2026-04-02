<%@ page import="com.bupt.tarecruitment.auth.AuthService" %>
<%@ page import="com.bupt.tarecruitment.auth.AuthValidator" %>
<%@ page import="com.bupt.tarecruitment.auth.TextFileUserRepository" %>
<%@ page import="com.bupt.tarecruitment.auth.UserAccount" %>
<%@ page import="com.bupt.tarecruitment.auth.UserRole" %>
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
    String selectedRole = request.getParameter("role") == null ? "" : request.getParameter("role");

    if ("POST".equalsIgnoreCase(request.getMethod())) {
        userIdValue = request.getParameter("userId") == null ? "" : request.getParameter("userId").trim();
        String passwordValue = request.getParameter("password") == null ? "" : request.getParameter("password");
        selectedRole = request.getParameter("role") == null ? "" : request.getParameter("role").trim();

        try {
            UserRole role = UserRole.valueOf(selectedRole);
            Path dataDirectory = findDataDirectory(application);
            AuthService service = new AuthService(new TextFileUserRepository(dataDirectory), new AuthValidator());
            UserAccount account = service.register(userIdValue, passwordValue, role);

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
    <title>Register | TA Recruitment</title>
    <link rel="stylesheet" href="assets/auth.css">
</head>
<body class="auth-body">
<div class="shell">
    <header class="topbar">
        <a class="brand" href="index.jsp">TA Recruitment</a>
        <nav class="nav">
            <a href="index.jsp">Home</a>
            <a href="login.jsp">Login</a>
            <a class="is-active" href="register.jsp">Register</a>
        </nav>
    </header>

    <main class="auth-layout">
        <section class="auth-panel">
            <p class="auth-label">New Account</p>
            <h1>Create your TA recruitment account</h1>
            <p class="auth-summary">
                Register with the same role and ID conventions used by the current console auth flow.
                This makes the page easy to connect to the existing `AuthService` later.
            </p>

            <% if (error != null && !error.isBlank()) { %>
            <div class="message message-error"><%= error %></div>
            <% } %>
            <% if (success != null && !success.isBlank()) { %>
            <div class="message message-success"><%= success %></div>
            <% } %>

            <form class="auth-form" action="register.jsp" method="post">
                <div class="field">
                    <label for="userId">User ID</label>
                    <input
                        id="userId"
                        name="userId"
                        type="text"
                        placeholder="e.g. ta101"
                        value="<%= userIdValue %>"
                        required
                    >
                    <div class="field-hint">Examples: <code>ta101</code>, <code>mo101</code>, <code>admin101</code>.</div>
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" placeholder="Create a password" required>
                </div>

                <div class="field">
                    <label for="role">Role</label>
                    <select id="role" name="role" required>
                        <option value="">Select a role</option>
                        <option value="APPLICANT" <%= "APPLICANT".equals(selectedRole) ? "selected" : "" %>>Applicant</option>
                        <option value="MO" <%= "MO".equals(selectedRole) ? "selected" : "" %>>Module Organiser</option>
                        <option value="ADMIN" <%= "ADMIN".equals(selectedRole) ? "selected" : "" %>>Admin</option>
                    </select>
                    <div class="field-hint">The selected role determines which userId prefix is valid.</div>
                </div>

                <div class="auth-meta">
                    <span>Designed to fit the current Sprint 1 demo scope.</span>
                    <a href="login.jsp">Already registered?</a>
                </div>

                <div class="auth-submit">
                    <button class="btn btn-primary" type="submit">Create Account</button>
                </div>
            </form>

            <p class="auth-foot">
                Recommended first release path: keep self-registration mainly for applicants, while organiser and
                admin accounts can still be provisioned from seeded data during demos.
            </p>
        </section>

        <aside class="side-stack">
            <section class="side-card">
                <h3>Role Rules</h3>
                <ul>
                    <li>Applicant IDs should start with <strong>ta</strong>.</li>
                    <li>Module organiser IDs should start with <strong>mo</strong>.</li>
                    <li>Admin IDs should start with <strong>admin</strong>.</li>
                </ul>
            </section>

            <section class="side-card">
                <h3>Why This Layout</h3>
                <p>
                    The page follows the same warm editorial style as your reference image: soft beige background,
                    rounded glass-like cards, serif headlines, and compact form blocks designed for a recruitment demo.
                </p>
            </section>
        </aside>
    </main>
</div>
</body>
</html>
