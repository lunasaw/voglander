# Export after `tb_export_task`

The legacy `tb_export_task` table and `/api/v1/exportTask/*` routes are removed
as a breaking 1.0.9 change. New export work must enter the durable business-task
engine through the trusted `DATA_EXPORT` `LongTaskHandler`.

The Handler owns a versioned payload and writes export-specific facts to a
domain detail table (for example, an export artifact/detail table) through its
completion participant. The generic `tb_biz_task`, execution, and event tables
hold scheduling, attempts, progress, result references, and sanitized audit
facts only. No new code may recreate `tb_export_task`, reuse its numeric IDs, or
expose the removed route family.

Before enabling a production export Handler, run the release preflight, retain
the native database backup, and verify that the old table is empty or that the
release owner has explicitly authorized its destructive removal.
