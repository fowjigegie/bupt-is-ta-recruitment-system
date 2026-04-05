# Iteration 2 Checklist

This file is a quick project-level review of Iteration 2.

Scope of this review:
- coarse functional check only
- build and existing smoke tests pass
- not a full edge-case audit

## Current overall status

Iteration 2 is partially complete.

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
Status: mostly done

What exists:
- applicant status page in `UI/InterviewInvitationPage.java`
- shows real application records and status values

Notes:
- good enough for demo

### `US-07 Search and Filter Jobs`
Status: mostly done

What exists:
- `UI/MoreJobsPage.java` supports pagination
- `UI/MoreJobsPage.java` supports sorting
- `UI/MoreJobsPage.java` supports keyword search
- `UI/MoreJobsPage.java` supports skill and organiser filters
- `com/bupt/tarecruitment/job/JobBrowseFilter.java` contains reusable filtering logic

Notes:
- search/filter core flow now exists
- deeper filter dimensions such as weekly hours or schedule can still be added later if needed

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
Status: mostly done

What exists:
- job management page in `UI/JobManagementPage.java`
- edit existing job through `UI/PostVacanciesPage.java`
- open/close status switching

Notes:
- looks usable in main flow

### `US-13 Review and Process Applications`
Status: mostly done

What exists:
- application review page in `UI/ApplicationReviewPage.java`
- organiser can set:
  - `SHORTLISTED`
  - `ACCEPTED`
  - `REJECTED`
- reviewer note is supported

Notes:
- this part is in relatively good shape

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

1. Applicant dashboard unread message count is hard-coded.
   File: `UI/DashboardPages.java`

1. Applicant dashboard chat entry does not always carry a job/MO context.
   File: `UI/DashboardPages.java`

2. `More Jobs` says `Sort by publish time`, but current sorting is actually based on the numeric part of `jobId`.
   File: `UI/MoreJobsPage.java`

## Recommended finish order

### Priority 1
- replace hard-coded unread message count with real data
- improve dashboard chat entry context
- rename or fix the `publish time` sorting behavior

### Priority 2
- add more smoke tests for Iteration 2 stories that still have no dedicated tests
- polish wording and consistency across JavaFX pages
