# topsort.kt — Claude Code

@AGENTS.md

## Shared Files

- Never modify `AGENTS.md` without explicit approval; it is shared with other AI tools.

## Claude Code

- When waiting the 5 minutes before an `--admin` merge, run the wait in the background and do
  **not** block on `TaskOutput`. Let the completion notification come to you so the session stays
  responsive.
