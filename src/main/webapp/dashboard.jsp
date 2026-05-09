<%@ page import="com.bupt.tarecruitment.auth.UserRole" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String currentUserId = (String) session.getAttribute("currentUserId");
    String currentDisplayName = (String) session.getAttribute("currentDisplayName");
    String currentRole = (String) session.getAttribute("currentUserRole");
    String currentStatus = (String) session.getAttribute("currentUserStatus");

    if (currentUserId == null || currentRole == null) {
        response.sendRedirect("login.jsp?error=Please+log+in+first.");
        return;
    }

    String roleCopy;
    if (UserRole.APPLICANT.name().equals(currentRole)) {
        roleCopy = "You can continue with profile creation, CV management, and job application steps.";
    } else if (UserRole.MO.name().equals(currentRole)) {
        roleCopy = "You can continue with posting vacancies and reviewing applicant materials.";
    } else {
        roleCopy = "You can continue with seeded account management and system-level oversight.";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard | TA Recruitment</title>
    <link rel="stylesheet" href="assets/auth.css">
</head>
<body class="auth-body">
<div class="shell">
    <header class="topbar">
        <a class="brand" href="index.jsp">TA Recruitment</a>
        <nav class="nav">
            <a href="index.jsp">Home</a>
            <a href="dashboard.jsp" class="is-active">Dashboard</a>
            <a href="logout.jsp">Logout</a>
        </nav>
    </header>

    <main>
        <section class="hero-grid">
            <div class="hero-copy">
                <p class="eyebrow">Authenticated Session</p>
                <h1 class="hero-title">Welcome back, <%= currentDisplayName %></h1>
                <p class="hero-subtitle">
                    Your JSP auth flow is now connected. The current session is being tracked in the web layer and
                    can already support a lightweight demo of login, registration, and logout.
                </p>
                <div class="hero-actions">
                    <a class="btn btn-primary" href="index.jsp">Back to Home</a>
                    <a class="btn btn-secondary" href="logout.jsp">Logout</a>
                </div>
            </div>

            <aside class="account-card">
                <h2>Session Summary</h2>
                <div class="account-list">
                    <div class="account-item"><strong>User ID:</strong> <%= currentUserId %></div>
                    <div class="account-item"><strong>Role:</strong> <%= currentRole %></div>
                    <div class="account-item"><strong>Status:</strong> <%= currentStatus %></div>
                </div>
            </aside>
        </section>

        <section class="meta-grid">
            <article class="meta-card">
                <h3>Role Entry</h3>
                <p><%= roleCopy %></p>
            </article>
            <article class="meta-card">
                <h3>Current Scope</h3>
                <p>
                    This dashboard is intentionally small: it proves the login/register UI is already linked together
                    and leaves room for you to connect applicant, organiser, and admin modules next.
                </p>
            </article>
        </section>
    </main>
</div>
</body>
</html>
