# TA Recruitment System Team Conventions

## 1. Purpose of this document

This file is the team's shared development contract.

It is not only a coding note. It also defines:

- how the project is organised
- who owns which long-term business direction
- who owns which Sprint 1 user story
- what shared data structures must stay stable
- what cross-module contracts other teammates can rely on
- what GitHub workflow the team should follow

If a change affects shared fields, statuses, IDs, storage format, login flow, or the main demo path, update this file in the same PR.

## 2. Current technical decision

- Current starter uses plain Java 21.
- The project does not depend on Maven, a database, or heavy frameworks.
- Data is stored in UTF-8 text files under `data/`.
- This matches the coursework requirement of either a stand-alone Java application or a lightweight Servlet/JSP application using simple text-file storage.
- The current scaffold is a stand-alone Java starter with separated business modules so the team can keep it as stand-alone or later add a Servlet/JSP UI layer without rewriting the core business modules.

## 3. Top-level structure

```text
.
|-- data/
|-- docs/
|   `-- TEAM_CONVENTIONS.md
|-- scripts/
|   |-- build.ps1
|   |-- run.ps1
|   `-- test.ps1
`-- src/
    |-- main/java/com/bupt/tarecruitment/
    `-- test/java/com/bupt/tarecruitment/
```

## 4. Folder meaning

- `src/main/java/`
  - main source code
- `src/test/java/`
  - test code
- `data/`
  - plain text storage files for demo and development
- `scripts/`
  - build, run, and test scripts
- `docs/`
  - project documents and team conventions
- `out/`
  - compiled output only, not source code, should not be committed

## 5. Project structure rule

The codebase stays organised by business module, even if ownership is discussed from two angles:

- long-term ownership by business direction
- short-term Sprint ownership by user story

Current module folders:

- `auth/`
- `applicant/`
- `job/`
- `application/`
- `admin/`
- `communication/`
- `recommendation/`
- `common/`
- `bootstrap/`

Meaning:

- Folders are for long-term code organisation.
- Long-term ownership defines who is the main person for a business area across multiple sprints.
- Sprint ownership is for short-term execution and responsibility.
- A story owner may touch more than one folder if their user story requires it.

## 6. Ownership model

### Long-term business direction ownership

- Group member 1: authentication and admin core
  - `US-00 User Registration and Login`
  - `US-14 Check TA Workload`
  - extra responsibility: shared login state, public navigation shell, base storage and build-script maintenance
- Group member 2: applicant profile and skills feedback
  - `US-01 Create Applicant Profile`
  - `US-05 Edit Applicant Profile`
  - `US-10 Missing Skills Feedback`
- Group member 3: CV, status view, and communication
  - `US-02 Submit CV`
  - `US-06 View Application Status`
  - `US-08 Contact Module Organiser`
- Group member 4: module organiser recruitment management
  - `US-11 Post a Job`
  - `US-12 Edit or Close a Job`
  - `US-13 Review and Process Applications`
- Group member 5: job discovery, search, and recommendation
  - `US-03 Browse Open Jobs`
  - `US-07 Search and Filter Jobs`
  - `US-09 Job Recommendation Based on Skills`
- Group member 6: application flow and conflict prevention
  - `US-04 Apply for a Job`
  - `US-15 Prevent Schedule Conflict`

Why this is the chosen split:

- workload is relatively balanced by story points
- related stories stay grouped by business flow
- cross-team dependency is reduced because each member owns a coherent user journey
- every member owns UI, logic, storage, and testing inside their own area

### Sprint 1 story ownership

Sprint 1 is still executed one story per person.

- Group member 1: `US-00 Account Registration and Login`
- Group member 2: `US-01 Create Applicant Profile`
- Group member 3: `US-02 Submit CV`
- Group member 4: `US-11 Post a Job`
- Group member 5: `US-03 Browse Open Jobs`
- Group member 6: `US-04 Apply for a Job`

UI rule for Sprint 1:

- The owner of a user story also owns that story's UI, validation, storage logic, and basic test coverage.
- Shared UI shell such as landing page, role entry point, or top-level navigation should be coordinated with the `US-00` owner.

## 7. Ownership-to-folder guidance

This section exists so each teammate knows which folders they will most likely touch across the project.

- Group member 1
  - mainly `auth/`, `admin/`, `common/`, `bootstrap/`
- Group member 2
  - mainly `applicant/`, and later parts of `recommendation/` for skills-gap logic
- Group member 3
  - mainly `applicant/`, `application/`, `communication/`
- Group member 4
  - mainly `job/`, `application/` for organiser-side review flow
- Group member 5
  - mainly `job/`, `recommendation/`
- Group member 6
  - mainly `application/`, `admin/` for schedule and workload conflict checks

Sprint 1 likely folder focus:

- Group member 1
  - `auth/`, `common/`, `bootstrap/`
- Group member 2
  - `applicant/`
- Group member 3
  - `applicant/`, possibly `common/` for CV storage helpers
- Group member 4
  - `job/`
- Group member 5
  - `job/`
- Group member 6
  - `application/`, but this story reads agreed contracts from `auth/`, `applicant/`, and `job/`

Rule:

- Do not move another member's responsibility into your own folder unless the story truly crosses modules.
- If a PR changes another member's main business area, that owner should review the PR.
- If a later story belongs to your long-term business direction, you are still expected to review or co-own cross-module changes that affect it.

## 8. Shared business contracts

### Roles

- `APPLICANT`
- `MO`
- `ADMIN`

### Account status

- `ACTIVE`
- `DISABLED`

### Job status

- `OPEN`
- `CLOSED`

### Application status

- `SUBMITTED`
- `SHORTLISTED`
- `ACCEPTED`
- `REJECTED`

### Time-slot format

- Unified string format: `DAY-HH:MM-HH:MM`
- Example: `MON-09:00-11:00`
- Multiple time slots inside one text-file field use `;` as the separator

### ID convention

- User ID example: `ta001`
- Profile ID example: `profile001`
- Job ID example: `job001`
- Application ID example: `application001`
- Message ID example: `message001`

The exact numbering strategy can be improved later, but the prefix convention should remain stable.

## 9. Data files and field order

The starter creates these files under `data/`:

- `users.txt`
  - `userId|passwordHash|role|displayName|status`
- `profiles.txt`
  - `profileId|userId|studentId|fullName|programme|yearOfStudy|educationLevel|skills(;)|availabilitySlots(;)|desiredPositions(;)|cvFileName`
- `jobs.txt`
  - `jobId|organiserId|title|moduleOrActivity|description|requiredSkills(;)|weeklyHours|scheduleSlots(;)|status`
- `applications.txt`
  - `applicationId|jobId|applicantUserId|status|submittedAt|reviewerNote`
- `messages.txt`
  - `messageId|jobId|senderUserId|receiverUserId|sentAt|content|readStatus`
- `cvs/`
  - stores applicant CV text files as `.txt`

Storage rules:

- Use UTF-8
- One record per line
- Avoid line breaks inside fields until escaping rules are added
- If a field contains multiple values, use `;`
- If a field contract changes, update both this file and the sample data in the same PR
- `cvFileName` stores a relative path, not an absolute local-machine path
- current agreed CV path example: `cvs/ta001/current.txt`

## 10. Cross-story contracts for Sprint 1

This is the most important interface section for parallel development.

### `US-00` must provide

Other stories depend on these agreements from the login story owner:

- role values must stay exactly `APPLICANT`, `MO`, `ADMIN`
- current user must always have `userId`, `displayName`, `role`
- login success must end with a role-aware entry point
- unauthenticated access must be detectable
- logout must clear current user context

Minimum logical contract:

- `login(userId, password)`
- `register(userId, password, role)`
- `getCurrentUser()`
- `logout()`

Even if the implementation changes later, these meanings should remain stable.

UI contract from `US-00`:

- there is a single login entry
- there is a registration path for applicant accounts
- after login, the user can be routed according to role
- other Sprint 1 pages may temporarily use a mocked current user until integration is finished

### `US-01` must provide

- profile is linked to exactly one `userId`
- required profile fields are saved in the agreed order in `profiles.txt`
- other stories may rely on profile lookup by `userId`
- `cvFileName` may be blank during `US-01` if `US-02` has not uploaded a CV yet
- `studentId` must be unique across applicant profiles and use 8 to 12 digits
- `fullName`, `programme`, `skills`, and `desiredPositions` use letters and spaces only in Sprint 1
- `yearOfStudy` must be between `1` and `4`
- `educationLevel` currently accepts `Graduated` or `Not Graduated`
- `availabilitySlots` is required and each slot must follow `DAY-HH:MM-HH:MM`

Minimum logical contract:

- `createProfile(profileData)`
- `getProfileByUserId(userId)`
- `updateProfile(profileData)`

### `US-02` must provide

- CV belongs to an applicant identified by `userId`
- CV storage uses the agreed `cvFileName` reference from the profile record
- the agreed reference is a relative path such as `cvs/ta001/current.txt`
- validation must reject empty or unsupported CV content according to the final story design
- repeated upload currently overwrites the applicant's current CV text file
- Sprint 1 CV input can be pasted text or a local `.txt` file import
- read-only CV review by applicant `userId` can be reused later by organiser-side stories

Minimum logical contract:

- `saveCv(userId, cvContentOrFileName)`
- `getCvReferenceByUserId(userId)`

### `US-03` must provide

- browse page shows only jobs with status `OPEN`
- each displayed job must expose `jobId`, title, module or activity, required skills, weekly hours, and schedule slots
- `US-04` relies on `jobId` and visible job status from this story

Minimum logical contract:

- `listOpenJobs()`
- `getJobById(jobId)`

### `US-11` must provide

- a new job must be saved using the agreed `jobs.txt` field order
- the organiser is recorded by `organiserId`
- a job created by this story is visible to `US-03` if status is `OPEN`

Minimum logical contract:

- `createJob(jobData)`
- `updateJobStatus(jobId, status)`

### `US-04` must provide

- application links exactly one applicant to exactly one job
- duplicate applications to the same job must be blocked
- only jobs with status `OPEN` can be applied to
- application status starts as `SUBMITTED`

Minimum logical contract:

- `applyToJob(applicantUserId, jobId)`
- `listApplicationsByApplicant(userId)`
- `hasApplied(applicantUserId, jobId)`

## 11. Temporary mock rule for parallel development

No one needs to wait for another story to be 100 percent complete before starting.

Allowed temporary strategy:

- use sample users such as `ta001`, `mo001`, `admin001`
- use mock current-user context during local development
- use fake or hard-coded navigation before final integration

Not allowed:

- changing shared field names without team agreement
- inventing a new role or status that conflicts with this file
- silently changing another story's data contract

## 12. UI ownership and consistency rule

Sprint 1 is not split into separate frontend and backend owners.

Rule:

- the story owner builds that story's page or interaction
- the story owner also builds that story's validation and storage logic
- the `US-00` owner coordinates shared entry pages and navigation
- visual style should stay simple and consistent across all Sprint 1 pages

For consistency, all Sprint 1 pages should at least keep these common elements aligned:

- page title style
- button labels
- error message wording
- field naming for shared concepts such as user ID, profile, job, and status

## 13. GitHub workflow

### Branching

- `main` must always stay in a runnable demo state
- development happens on feature branches
- recommended branch naming:
  - `feature/us00-auth-login`
  - `feature/us01-applicant-profile`
  - `feature/us02-submit-cv`
  - `feature/us03-browse-jobs`
  - `feature/us04-apply-job`
  - `feature/us11-post-job`
  - `feature/us14-check-workload`
  - `feature/us15-prevent-schedule-conflict`

### Issues

- create one GitHub Issue per user story
- create an extra Issue for cross-cutting work such as storage refactor, integration, or testing if needed
- every PR should mention or close an Issue

Example:

- Issue title: `US-04 Apply for a Job`
- branch: `feature/us04-apply-job`
- PR body line: `Closes #12`

### Pull requests

- one story or one tightly related change per PR
- PR title should include the story ID
- at least one teammate should review before merging
- if a PR changes another owner's main area, that owner should review it

### Commit style

- keep commits small and meaningful
- recommended style:
  - `feat: implement applicant profile save flow`
  - `feat: add open job list and job detail view`
  - `test: add smoke test for startup data files`
  - `docs: update auth and application contract`

## 14. Definition of done

A task is not done unless all of the following are true:

- code is placed in the correct module folder
- the project still builds with `scripts/build.ps1`
- sample data or storage contract is updated if needed
- basic validation or error handling is present
- the related documentation is updated if the contract changed
- the change is submitted through a branch and PR, not directly to `main`
- the owner can demonstrate the story in a live walkthrough

## 15. Shared files that need extra caution

- `docs/TEAM_CONVENTIONS.md`
- `src/main/java/com/bupt/tarecruitment/common/`
- `src/main/java/com/bupt/tarecruitment/auth/`
- sample file format under `data/`
- any future top-level app entry or route dispatcher

If one of these changes, notify the whole team before merging so nobody builds on an outdated contract.
