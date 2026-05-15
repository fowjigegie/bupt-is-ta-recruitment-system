# US02/US04 Acceptance Guide

## Build and Run

- Build only:
  - `powershell -ExecutionPolicy Bypass -File scripts\build.ps1`
- Run app (show command help):
  - `powershell -ExecutionPolicy Bypass -File scripts\run.ps1`
- Run existing smoke test:
  - `powershell -ExecutionPolicy Bypass -File scripts\test.ps1`
- Run US02/US04 smoke test:
  - `powershell -ExecutionPolicy Bypass -File scripts\test-us02-us04.ps1`

## US02 and US04 Scope

- `US-02`: CV library management (create/import/list/load CV).
- `US-04`: Apply to a job with one selected CV.
- Product flow decision:
  - Normal submission path is `US-04`.
  - The `Legacy Attach` action in `US-02` is kept only for backfill/compatibility records.

## Demo Data (default `data/`)

- Applicant user: `ta001` (profile exists).
- Existing CVs: `cv001`, `cv002`.
- Jobs:
  - `job001` OPEN (already applied by `ta001`)
  - `job002` OPEN (already applied by `ta001`)
  - `job003` OPEN (recommended for US04 success demo)

## Acceptance Evidence

- Automated regression command:
  - `powershell -ExecutionPolicy Bypass -File scripts\test-us02-us04.ps1`
- Expected pass marker:
  - `US02/US04 smoke test passed.`

## Manual Demo Checklist

1. US02 CV library
   - Run: `powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us02-ui`
   - Enter `ta001`, title, CV content, then click `Create CV`.
   - Click `List User CVs` and verify the new `cvId` appears.
2. US04 apply with selected CV
   - Run: `powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us04-ui job003 ta001`
   - Click `APPLY`, choose a CV, confirm.
   - Verify success dialog contains `applicationId`, `jobId`, `applicantUserId`, and selected `cvId`.
3. US04 duplicate protection
   - Re-run previous US04 command for the same `job003` and `ta001`.
   - Try applying again and verify duplicate-application error appears.
