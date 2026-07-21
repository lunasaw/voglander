-- Validate the durable business-task core objects in the current SQLite database.
WITH expected(object_type, object_name) AS (
    VALUES
        ('table', 'tb_biz_task'),
        ('table', 'tb_biz_task_execution'),
        ('table', 'tb_biz_task_event'),
        ('index', 'uk_biz_task_task_id'),
        ('index', 'uk_biz_task_idempotency'),
        ('index', 'idx_biz_task_due'),
        ('index', 'idx_biz_task_type_state'),
        ('index', 'idx_biz_task_owner'),
        ('index', 'idx_biz_task_biz_key'),
        ('index', 'idx_biz_task_subject'),
        ('index', 'uk_biz_task_execution_id'),
        ('index', 'uk_biz_task_execution_plan'),
        ('index', 'idx_biz_task_execution_task'),
        ('index', 'idx_biz_task_execution_pending'),
        ('index', 'idx_biz_task_execution_lease'),
        ('index', 'idx_biz_task_execution_retry_origin'),
        ('index', 'uk_biz_task_event_id'),
        ('index', 'uk_biz_task_event_dedupe'),
        ('index', 'idx_biz_task_event_task'),
        ('index', 'idx_biz_task_event_execution'),
        ('index', 'idx_biz_task_event_type')
)
SELECT expected.object_type,
       expected.object_name,
       EXISTS (
           SELECT 1 FROM sqlite_master object
           WHERE object.type = expected.object_type AND object.name = expected.object_name
       ) AS object_exists
FROM expected
ORDER BY expected.object_type DESC, expected.object_name;
