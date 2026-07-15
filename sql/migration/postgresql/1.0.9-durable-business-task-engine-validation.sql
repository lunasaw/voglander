-- Validate the durable business-task core objects in the current PostgreSQL schema.
WITH expected(object_type, object_name) AS (
    VALUES
        ('TABLE', 'tb_biz_task'),
        ('TABLE', 'tb_biz_task_execution'),
        ('TABLE', 'tb_biz_task_event'),
        ('INDEX', 'uk_biz_task_task_id'),
        ('INDEX', 'uk_biz_task_idempotency'),
        ('INDEX', 'idx_biz_task_due'),
        ('INDEX', 'idx_biz_task_type_state'),
        ('INDEX', 'idx_biz_task_owner'),
        ('INDEX', 'idx_biz_task_biz_key'),
        ('INDEX', 'idx_biz_task_subject'),
        ('INDEX', 'uk_biz_task_execution_id'),
        ('INDEX', 'uk_biz_task_execution_plan'),
        ('INDEX', 'idx_biz_task_execution_task'),
        ('INDEX', 'idx_biz_task_execution_pending'),
        ('INDEX', 'idx_biz_task_execution_lease'),
        ('INDEX', 'idx_biz_task_execution_retry_origin'),
        ('INDEX', 'uk_biz_task_event_id'),
        ('INDEX', 'uk_biz_task_event_dedupe'),
        ('INDEX', 'idx_biz_task_event_task'),
        ('INDEX', 'idx_biz_task_event_execution'),
        ('INDEX', 'idx_biz_task_event_type')
)
SELECT expected.object_type,
       expected.object_name,
       CASE
           WHEN expected.object_type = 'TABLE' THEN EXISTS (
               SELECT 1 FROM information_schema.tables t
               WHERE t.table_schema = current_schema() AND t.table_name = expected.object_name)
           ELSE EXISTS (
               SELECT 1 FROM pg_indexes i
               WHERE i.schemaname = current_schema() AND i.indexname = expected.object_name)
           END AS object_exists
FROM expected
ORDER BY expected.object_type DESC, expected.object_name;
