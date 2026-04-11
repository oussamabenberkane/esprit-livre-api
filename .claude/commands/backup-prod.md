---
description: Snapshot the Esprit Livre prod server to ~/backups/el/
---

Run the prod backup script and report the result. Pass through any args the user provided (e.g. `--prune`).

Execute:

```bash
scripts/backup-prod.sh $ARGUMENTS
```

After it finishes, summarize: location, size, elapsed, and whether any old backups were pruned or flagged for pruning.
