<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String currentUserId = (String) session.getAttribute("currentUserId");
    String currentDisplayName = (String) session.getAttribute("currentDisplayName");
    String currentRole = (String) session.getAttribute("currentUserRole");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TA Recruitment</title>
    <link rel="stylesheet" href="assets/auth.css">
</head>
<body class="auth-body">
<div class="shell">
    <header class="topbar">
        <a class="brand" href="index.jsp">TA Recruitment</a>
        <nav class="nav">
            <a class="is-active" href="index.jsp">Home</a>
            <% if (currentUserId == null) { %>
            <a href="login.jsp">Login</a>
            <a href="register.jsp">Register</a>
            <% } else { %>
            <a href="dashboard.jsp">Dashboard</a>
            <a href="logout.jsp">Logout</a>
            <% } %>
        </nav>
    </header>

    <main>
        <% if (currentUserId != null) { %>
        <section class="session-banner">
            <div class="session-copy">
                Signed in as <strong><%= currentDisplayName %></strong> with <strong><%= currentRole %></strong> access.
                You can continue to the dashboard or sign out from here.
            </div>
            <div class="session-actions">
                <a class="btn btn-primary" href="dashboard.jsp">Open Dashboard</a>
                <a class="ghost-link" href="logout.jsp">Logout</a>
            </div>
        </section>
        <% } %>

        <section class="hero-grid">
            <div class="hero-copy">
                <p class="eyebrow">Sprint 1 Demo</p>
                <h1 class="hero-title">TA Recruitment System</h1>
                <p class="hero-subtitle">
                    A lightweight browser-based recruitment workflow for applicants and module organisers,
                    adapted from the current plain Java demo and text-file storage project structure.
                </p>
                <div class="hero-actions">
                    <% if (currentUserId == null) { %>
                    <a class="btn btn-primary" href="login.jsp">Login</a>
                    <a class="btn btn-secondary" href="register.jsp">Register as Applicant</a>
                    <% } else { %>
                    <a class="btn btn-primary" href="dashboard.jsp">Continue Session</a>
                    <a class="btn btn-secondary" href="logout.jsp">Logout</a>
                    <% } %>
                </div>
            </div>

            <aside class="account-card">
                <h2>Demo Accounts</h2>
                <div class="account-list">
                    <div class="account-item"><strong>Applicant:</strong> ta001 / demo-ta-password</div>
                    <div class="account-item"><strong>Module organiser:</strong> mo001 / demo-mo-password</div>
                    <div class="account-item"><strong>Admin:</strong> admin001 / demo-admin-password</div>
                </div>
            </aside>
        </section>

        <section class="feature-grid">
            <article class="info-card">
                <h2>Applicant Flow</h2>
                <p>
                    Create a profile, save a CV reference, browse open jobs, and submit an application in one place.
                    The JSP version keeps the same userId conventions as the current console workflow.
                </p>
            </article>

            <article class="info-card">
                <h2>Organiser Flow</h2>
                <p>
                    Post a new TA opening, review your existing jobs, and keep the Sprint 1 demo path aligned with
                    the shared auth, applicant, and application contracts already present in the repository.
                </p>
            </article>
        </section>
    </main>
</div>
</body>
</html>
