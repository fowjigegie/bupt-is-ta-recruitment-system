# UI Startup Guide

This project now uses the JavaFX UI under `src/main/java/com/bupt/tarecruitment/ui/` as the main demo path.

The JSP web prototype under `src/main/webapp/` is still kept for later work, but it is not the main desktop demo route.

## Main UI entry

Use one of these two ways:

1. In VS Code:
   - Open `Run and Debug`
   - Choose `Run JavaFX LoginPage`
   - Start from `com.bupt.tarecruitment.ui.LoginPage`

2. In PowerShell:
   - Run `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1`
   - To launch a specific page, run for example:
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 com.bupt.tarecruitment.ui.LoginPage`
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 com.bupt.tarecruitment.ui.MoreJobsPage`
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 com.bupt.tarecruitment.ui.PostVacanciesPage`

## Current JavaFX pages used in the main demo

- `com.bupt.tarecruitment.ui.LoginPage`
- `com.bupt.tarecruitment.ui.RegisterPage`
- `com.bupt.tarecruitment.ui.DashboardPages`
- `com.bupt.tarecruitment.ui.ResumeDatabasePage`
- `com.bupt.tarecruitment.ui.MoreJobsPage`
- `com.bupt.tarecruitment.ui.JobDetailPage`
- `com.bupt.tarecruitment.ui.InterviewInvitationPage`
- `com.bupt.tarecruitment.ui.ModuleOrganizerDashboardPage`
- `com.bupt.tarecruitment.ui.PostVacanciesPage`
- `com.bupt.tarecruitment.ui.JobManagementPage`
- `com.bupt.tarecruitment.ui.ApplicationReviewPage`
- `com.bupt.tarecruitment.ui.AdminDashboardPage`

These pages are connected by `com.bupt.tarecruitment.ui.NavigationManager` and use real services through `com.bupt.tarecruitment.ui.UiServices`.

## Old desktop UI status

The older Swing demo pages have been removed from `src/main/java` and are no longer the supported main demo path.

The separate JSP web resources remain under `src/main/webapp/` for later coursework work.

## JavaFX environment reminder

Each teammate must configure JavaFX on their own machine.

Typical VS Code local settings:

- JDK 21
- JavaFX SDK path in `.vscode/settings.json`
- JavaFX VM args in `.vscode/launch.json`

If a teammate gets errors like `NoClassDefFoundError: Stage`, it usually means JavaFX is not configured correctly on their machine.

## Demo accounts

- Applicant already used for historical demo data:
  - `ta001 / 123456`
- Fresh applicant recommended for application walkthrough:
  - `ta002 / 123456`
- Module organiser:
  - `mo001 / 123456`
- Admin:
  - `admin001 / 123456`
