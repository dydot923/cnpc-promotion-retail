# Codex Project Instructions

## Git Workflow

- At the start of a coding task, run `git status --short` and note any pre-existing changes.
- Do not revert, overwrite, stage, or commit user changes that were already present unless the user explicitly asks.
- After making changes, inspect the exact diff with `git diff -- <paths>` for files touched in the current task.
- Run relevant checks before committing when practical.
- Stage only the paths changed for the current task. Prefer explicit path staging such as `git add -- AGENTS.md`; avoid `git add -A` when the worktree already has unrelated changes.
- Inspect `git diff --cached` before committing.
- Commit the current task with a clear Conventional Commits-style message, for example `feat: add checkout confirmation flow` or `fix: handle import validation errors`.
- Report the commit hash in the final response.
- Keep version control local by default. Do not push to a remote repository unless the user explicitly asks.

## Verification Commands

- Backend changes: from `backend`, run `mvn test` when practical.
- Frontend changes: from `frontend`, run `npm test` and `npm run build` when practical.
- Documentation-only or instruction-only changes do not need the full test suite, but should still be reviewed with `git diff`.

## Safety Rules

- Never commit secrets, local credentials, generated dependency folders, build outputs, or unrelated large files.
- If tests fail, explain the failure and do not commit unless the user explicitly asks to preserve the failing state.
- If a clean commit cannot be made without including unrelated changes, stop and ask the user how to proceed.
- Do not rewrite Git history, force-push, reset hard, or delete branches unless the user explicitly requests it.
