# Durable Business Task Baseline and Rollback Evidence

Captured at `2026-07-14 23:35:58 +0800` before destructive legacy-task removal.

## Source baseline

- Repository: `voglander`
- Branch: `dev`
- HEAD: `e91893b` (`docs(task): design durable business task engine`)
- The working tree already contains uncommitted PostgreSQL, durable-task, and image-asset work. Those changes are user-owned and must not be reset or overwritten.

## Legacy SQLite data

| Database | `tb_export_task` rows | Rollback evidence |
| --- | ---: | --- |
| `voglander-web/app.db` | 0 | `.superpowers/backups/durable-business-task-baseline/voglander-web-app-before-durable.db` |
| workspace-root `app.db` | N/A | Zero-byte placeholder; no `tb_export_task` table |

The SQLite backup SHA-256 is
`2867bc1ce637ed30f0fea2cf396e3072cd1ab4dbf4ae41254bb3323811560ff7`.
Runtime databases and their backups are ignored by Git and must not be committed.

## MySQL and PostgreSQL deployment targets

No live MySQL or PostgreSQL deployment connection is configured in this workspace, so this baseline does not claim a row count for either external environment. Before running the incremental migration in any such environment, the release operator must:

1. create and verify a database-native backup;
2. run `SELECT COUNT(*) AS export_task_rows FROM tb_export_task;`;
3. record the target, timestamp, backup identifier, and count in the release report;
4. stop when the count is non-zero unless the release owner supplies the explicit destructive authorization required by the migration gate.

Fresh-schema test databases are not deployment targets and do not substitute for this environment-level check.

## Legacy API consumers

- The backend still exposes `/api/v1/exportTask/*` through `ExportTaskController` and its Manager/Service/Repository chain.
- No runtime file under `vue-vben-admin/apps/web-antd/src` calls the legacy endpoint.
- The remaining frontend references are generated or historical documentation under `apps/web-antd/api` and `apps/web-antd/doc`.
- Backend API documentation under `api/voglander-api.md` still publishes the legacy contract.

The retirement task must remove the backend chain and published API documentation. There is no frontend runtime compatibility client to migrate.

## Rollback boundary

Before the legacy table is dropped in a deployment, disable the durable scheduler/dispatcher and take a new environment-specific backup. Rolling back to a binary that still requires `tb_export_task` also requires restoring that backup; recreating an empty table is not a data rollback.
