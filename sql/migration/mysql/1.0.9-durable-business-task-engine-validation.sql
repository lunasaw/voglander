-- Validate the durable business-task core objects in the current MySQL schema.
SELECT expected.object_type,
       expected.object_name,
       CASE
           WHEN expected.object_type = 'TABLE' THEN EXISTS (
               SELECT 1 FROM information_schema.tables t
               WHERE t.table_schema = DATABASE() AND t.table_name = expected.object_name)
           ELSE EXISTS (
               SELECT 1 FROM information_schema.statistics s
               WHERE s.table_schema = DATABASE() AND s.index_name = expected.object_name)
           END AS object_exists
FROM (
    SELECT 'TABLE' AS object_type, 'tb_biz_task' AS object_name
    UNION ALL SELECT 'TABLE', 'tb_biz_task_execution'
    UNION ALL SELECT 'TABLE', 'tb_biz_task_event'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_task_id'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_idempotency'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_due'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_type_state'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_owner'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_biz_key'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_subject'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_execution_id'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_execution_plan'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_execution_task'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_execution_pending'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_execution_lease'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_execution_retry_origin'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_event_id'
    UNION ALL SELECT 'INDEX', 'uk_biz_task_event_dedupe'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_event_task'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_event_execution'
    UNION ALL SELECT 'INDEX', 'idx_biz_task_event_type'
) expected
ORDER BY expected.object_type DESC, expected.object_name;
