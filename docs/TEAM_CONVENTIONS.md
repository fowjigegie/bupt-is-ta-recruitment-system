# TA Recruitment System Team Conventions

## 1. Current starter decision

- Current starter uses plain Java 21 and does not depend on Maven or a database.
- Data is stored in UTF-8 text files under `data/`, which matches the coursework constraint of using simple text formats.
- The current scaffold is a lightweight standalone Java starter. Its core modules are intentionally separated so the team can later keep it as a standalone app or add a Servlet/JSP presentation layer without rewriting the business modules.

## 2. Top-level structure

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

## 3. Module ownership

- Group member 1: `common/` and `auth/`
- Group member 2: `applicant/`
- Group member 3: `job/`
- Group member 4: `application/`
- Group member 5: `admin/`
- Group member 6: `communication/`, `recommendation/`, and integration smoke tests

Rule:

- Each member is the primary owner of their module folder.
- Other members may edit another module only when their user story requires it, and the module owner should review that PR.
- Shared code under `common/` should not be changed casually. Changes there must be announced in the group chat or issue before merging.

## 4. Shared business contracts

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
- Multiple time slots inside one text-file field use `;` as the separator.

### ID convention

- User ID: existing school or system login ID, for example `ta001`
- Profile ID: `profile001`
- Job ID: `job001`
- Application ID: `application001`
- Message ID: `message001`

The exact numbering strategy can be improved later, but the prefix convention should remain stable.

## 5. Data files and field order

The starter already creates these files under `data/`:

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

Rules:

- Use UTF-8.
- One record per line.
- Keep single-line text fields simple for now. Avoid line breaks inside a field until the storage owner adds escaping support.
- If a field contains multiple values, use `;`.

## 6. Development flow

### Branching

- `main` must always stay in a runnable demo state.
- Development should happen on feature branches.
- Recommended branch naming:
  - `feature/us00-auth-login`
  - `feature/us01-applicant-profile`
  - `feature/us11-post-job`

### Pull requests

- One user story or one tightly related change per PR.
- PR title should include the story ID.
- At least one teammate should review before merging.
- If a PR changes another member's owned module, that module owner should review it.

### Commit style

- Keep commits small and meaningful.
- Recommended message style:
  - `feat: add applicant profile model and repository contract`
  - `test: add smoke test for data bootstrap`
  - `docs: define shared file format and module boundaries`

## 7. Definition of done

A task should not be considered done unless it includes all of the following:

- Code is placed in the correct module folder.
- The project still builds with `scripts/build.ps1`.
- Demo data or storage impact is updated if needed.
- Basic validation or error path is considered.
- The related documentation or comments are updated when the contract changes.
- The change is submitted through a branch and PR instead of being committed directly to `main`.

## 8. Immediate Sprint 1 working rule

- Do not wait for one member to finish everything before others start.
- First agree on contracts, then develop in parallel.
- If a real dependency is not finished yet, use mock data and keep the interface stable.

Recommended parallel start:

- Member 1 starts `auth/` and shared bootstrap.
- Member 2 starts profile and CV module against the agreed user ID format.
- Member 3 starts job posting and job browse module against the agreed job format.
- Member 4 starts apply/status flow against the agreed application status enum.
- Member 5 starts workload and conflict rules using sample job and application data.
- Member 6 starts message/recommendation placeholders and smoke/integration testing.

## 9. Shared files that need extra caution

- `docs/TEAM_CONVENTIONS.md`
- `src/main/java/com/bupt/tarecruitment/common/`
- Data file format under `data/`
- Any future top-level app entry or route dispatcher

If one of these changes, notify the whole team before merging so nobody builds on an outdated contract.
