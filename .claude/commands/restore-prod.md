---
description: Restore an Esprit Livre prod backup (dry-run by default)
---

Run the prod restore script and relay its output faithfully. Pass through any arguments the user provided (e.g. `--postgres --execute`, `--all --execute --yes`, or a specific backup dir).

Execute:

```bash
scripts/restore-prod.sh $ARGUMENTS
```

Do not add or remove any flags. If the user didn't pass `--execute`, the script stays in dry-run mode — don't "helpfully" add it. If the script prompts for a typed-timestamp confirmation, forward it to the user verbatim.

After it finishes, summarize: which components were applied, which (if any) failed, and whether prod is in a clean or mixed state.
