---
description: Snapshot the Esprit Livre prod server to Desktop/app-backups
---

Run the prod backup script and report the result. Pass through any args the user provided (e.g. `--prune`).

Execute:

```bash
scripts/backup-prod.sh $ARGUMENTS
```

Backups land in `~/OneDrive/Desktop/app-backups/<timestamp>/` (override with `EL_BACKUP_ROOT`). Each snapshot contains: app DB dump (`postgres-app.dump`), Keycloak DB dump (`postgres-keycloak.dump`), postgres roles (`postgres-globals.sql`), live Keycloak realm export, api media volume, host config (`/root/esprit-livre` incl. `.env` + nginx), `/etc/letsencrypt`, cert-renewal infra (root crontab, renew script, `/root/.acme.sh`), and a sha256 `MANIFEST.txt` that the script verifies after transfer.

After it finishes, summarize: location, size, elapsed, and whether any old backups were pruned or flagged for pruning.