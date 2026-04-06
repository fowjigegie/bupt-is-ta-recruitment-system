# Iteration 2 Checklist

This file is a quick project-level review of Iteration 2.

Scope of this review:
- coarse functional check only
- build and existing smoke tests pass
- not a full edge-case audit

## Current overall status

Iteration 2 is basically complete.

Stories that look basically implemented:
- `US-06 View Application Status`
- `US-07 Search and Filter Jobs`
- `US-08 Contact Module Organiser`
- `US-09 Job Recommendation Based on Skills`
- `US-10 Missing Skills Feedback`
- `US-12 Edit or Close a Job`
- `US-13 Review and Process Applications`
- `US-14 Check TA Workload`
- `US-15 Prevent Schedule Conflict`

## Story-by-story view

### `US-06 View Application Status`
Status: done enough for demo

What exists:
- applicant status page in `UI/InterviewInvitationPage.java`
- shows real application records and status values
- smoke test in `src/test/java/com/bupt/tarecruitment/US06SmokeTest.java`

Notes:
- status mapping and applicant-side listing are now covered by smoke testing

### `US-07 Search and Filter Jobs`
Status: mostly done

What exists:
- `UI/MoreJobsPage.java` supports a scrollable job list
- `UI/MoreJobsPage.java` supports sorting
- `UI/MoreJobsPage.java` supports keyword search
- `UI/MoreJobsPage.java` supports module, activity, skill, organiser, and time-slot filters
- `com/bupt/tarecruitment/job/JobBrowseFilter.java` contains reusable filtering logic
- keyword search can also match MO display names

Notes:
- search/filter core flow now exists
- current sorting still uses the numeric part of `jobId`, not a true publish timestamp

### `US-08 Contact Module Organiser`
Status: mostly done

What exists:
- messaging page in `UI/MessagesPage.java`
- message service in `com/bupt/tarecruitment/communication/MessageService.java`
- chat can open from jobs pages

Notes:
- main flow exists
- still needs small polish in dashboard entry behavior

### `US-09 Job Recommendation Based on Skills`
Status: mostly done

What exists:
- dashboard section called recommended jobs
- `recommendation/RecommendationResult.java`
- `recommendation/RecommendationService.java`
- dashboard now ranks jobs using applicant skills and desired positions

Notes:
- recommendation now works at a practical demo level
- this can still be improved later with stronger scoring or richer explanations

### `US-10 Missing Skills Feedback`
Status: mostly done

What exists:
- `recommendation/MissingSkillsFeedbackService.java`
- skill-gap preview in `UI/MoreJobsPage.java`
- detailed skill-gap feedback card in `UI/JobDetailPage.java`

Notes:
- applicant can now see matched skills, missing skills, and coverage percentage for a job

### `US-12 Edit or Close a Job`
Status: done enough for demo

What exists:
- job management page in `UI/JobManagementPage.java`
- edit existing job through `UI/PostVacanciesPage.java`
- open/close status switching
- smoke test in `src/test/java/com/bupt/tarecruitment/US12SmokeTest.java`

Notes:
- edit and close flows are now covered by smoke testing

### `US-13 Review and Process Applications`
Status: done enough for demo

What exists:
- application review page in `UI/ApplicationReviewPage.java`
- organiser can set:
  - `SHORTLISTED`
  - `ACCEPTED`
  - `REJECTED`
- reviewer note is supported
- smoke test in `src/test/java/com/bupt/tarecruitment/US13SmokeTest.java`

Notes:
- review flow is covered at the service level; UI layout can still be improved later if needed

### `US-14 Check TA Workload`
Status: done enough for demo

What exists:
- admin workload dashboard in `UI/AdminDashboardPage.java`
- workload calculation in `com/bupt/tarecruitment/admin/AdminWorkloadService.java`
- smoke test in `src/test/java/com/bupt/tarecruitment/US14SmokeTest.java`

Notes:
- one of the more complete Iteration 2 stories

### `US-15 Prevent Schedule Conflict`
Status: mostly done

What exists:
- `application/ScheduleConflictGuard.java`
- apply flow blocks new applications that clash with already accepted assignments
- organiser acceptance flow blocks `ACCEPTED` transitions that would create a clash

Notes:
- admin workload page still detects any legacy conflict data, but new conflicts are now prevented in the main workflow

## Known obvious issues

These are not necessarily blockers, but they are visible:

1. `More Jobs` sorting is based on the numeric part of `jobId`, not a real publish timestamp.
   File: `UI/MoreJobsPage.java`

## Recommended finish order

### Priority 1
- rename or strengthen the current `jobId`-based sort behavior

### Priority 2
- polish wording and consistency across JavaFX pages
