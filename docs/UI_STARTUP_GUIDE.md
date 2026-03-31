# UI Startup Guide

This project now uses the JavaFX UI under `src/main/java/UI/` as the main Sprint 1 demo path.

Do not switch the demo back to the older Swing pages unless the team explicitly decides to do that.

## Main UI entry

Use one of these two ways:

1. In VS Code:
   - Open `Run and Debug`
   - Choose `Run JavaFX LoginPage`
   - Start from `UI.LoginPage`

2. In PowerShell:
   - Run `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1`
   - To launch a specific page, run for example:
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 UI.LoginPage`
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 UI.MoreJobsPage`
     - `powershell -ExecutionPolicy Bypass -File scripts/run-javafx.ps1 UI.PostVacanciesPage`

## Current JavaFX pages used in the main demo

- `UI.LoginPage`
- `UI.RegisterPage`
- `UI.DashboardPages`
- `UI.ResumeDatabasePage`
- `UI.MoreJobsPage`
- `UI.JobDetailPage`
- `UI.InterviewInvitationPage`
- `UI.ModuleOrganizerDashboardPage`
- `UI.PostVacanciesPage`
- `UI.JobManagementPage`
- `UI.ApplicationReviewPage`
- `UI.AdminDashboardPage`

These pages are connected by `UI.NavigationManager` and use real services through `UI.UiServices`.

## Old UI that should not be used as the main demo

The following code is legacy or auxiliary and is not the main integrated Sprint 1 UI:

- `com/bupt/tarecruitment/MainLauncherSwing`
- `com/bupt/tarecruitment/auth/AuthSwingDemo`
- `com/bupt/tarecruitment/applicant/ApplicantProfileSwingDemo`
- `com/bupt/tarecruitment/applicant/ApplicantCvSwingDemo`
- `com/bupt/tarecruitment/applicant/ApplicantCvReviewSwingDemo`
- `com/bupt/tarecruitment/job/JobPostingSwingDemo`

These older Swing pages may still exist for backward compatibility, but they are not the team's primary demo path anymore.

## JavaFX environment reminder

Each teammate must configure JavaFX on their own machine.

Typical VS Code local settings:

- JDK 21
- JavaFX SDK path in `.vscode/settings.json`
- JavaFX VM args in `.vscode/launch.json`

If a teammate gets errors like `NoClassDefFoundError: Stage`, it usually means JavaFX is not configured correctly on their machine.

## Demo accounts

- Applicant already used for historical demo data:
  - `ta001 / demo-ta-password`
- Fresh applicant recommended for application walkthrough:
  - `ta002 / demo-ta2-password`
- Module organiser:
  - `mo001 / demo-mo-password`
- Admin:
  - `admin001 / demo-admin-password`
