-- Voglander 1.0.9 durable business-task core migration (SQLite)
-- Additive and repeatable. Legacy export removal is a separate guarded release action.

CREATE TABLE IF NOT EXISTS tb_biz_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    task_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    task_mode VARCHAR(32) NOT NULL,
    schedule_start_time DATETIME,
    schedule_end_time DATETIME,
    interval_seconds INTEGER,
    next_plan_time DATETIME,
    schedule_version INTEGER NOT NULL DEFAULT 1,
    state VARCHAR(32) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    last_execution_id VARCHAR(64),
    last_execute_time DATETIME,
    completed_time DATETIME,
    planned_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    missed_count INTEGER NOT NULL DEFAULT 0,
    cancelled_count INTEGER NOT NULL DEFAULT 0,
    progress_current INTEGER NOT NULL DEFAULT 0,
    progress_total INTEGER NOT NULL DEFAULT 0,
    progress_message VARCHAR(512),
    progress_revision INTEGER NOT NULL DEFAULT 0,
    biz_key VARCHAR(128),
    subject_type VARCHAR(64),
    subject_id VARCHAR(128),
    payload TEXT NOT NULL,
    payload_version INTEGER NOT NULL,
    result_ref_type VARCHAR(64),
    result_ref_id VARCHAR(128),
    result_summary TEXT,
    last_failure_code VARCHAR(64),
    last_failure_message VARCHAR(512),
    origin_task_id VARCHAR(64),
    origin_execution_id VARCHAR(64),
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    organization_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_task_id ON tb_biz_task(task_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_idempotency ON tb_biz_task(owner_type,owner_id,task_type,idempotency_key);
CREATE INDEX IF NOT EXISTS idx_biz_task_due ON tb_biz_task(state,next_plan_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_type_state ON tb_biz_task(task_type,state,create_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_owner ON tb_biz_task(owner_type,owner_id,create_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_biz_key ON tb_biz_task(biz_key,task_type);
CREATE INDEX IF NOT EXISTS idx_biz_task_subject ON tb_biz_task(subject_type,subject_id,create_time);

CREATE TABLE IF NOT EXISTS tb_biz_task_execution (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    schedule_version INTEGER NOT NULL DEFAULT 1,
    planned_at DATETIME NOT NULL,
    deadline_at DATETIME,
    state VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    next_attempt_time DATETIME,
    started_at DATETIME,
    heartbeat_at DATETIME,
    finished_at DATETIME,
    claim_token VARCHAR(128),
    worker_node VARCHAR(128),
    lease_until DATETIME,
    progress_current INTEGER NOT NULL DEFAULT 0,
    progress_total INTEGER NOT NULL DEFAULT 0,
    progress_message VARCHAR(512),
    progress_revision INTEGER NOT NULL DEFAULT 0,
    result_ref_type VARCHAR(64),
    result_ref_id VARCHAR(128),
    result_summary TEXT,
    failure_code VARCHAR(64),
    failure_message VARCHAR(512),
    retryable INTEGER NOT NULL DEFAULT 0,
    retry_origin_execution_id VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_execution_id ON tb_biz_task_execution(execution_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_execution_plan ON tb_biz_task_execution(task_id,schedule_version,planned_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_task ON tb_biz_task_execution(task_id,planned_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_pending ON tb_biz_task_execution(state,next_attempt_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_lease ON tb_biz_task_execution(state,lease_until);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_retry_origin ON tb_biz_task_execution(retry_origin_execution_id);

CREATE TABLE IF NOT EXISTS tb_biz_task_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    execution_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32),
    attempt_no INTEGER,
    progress_current INTEGER,
    progress_total INTEGER,
    progress_message VARCHAR(512),
    failure_code VARCHAR(64),
    failure_message VARCHAR(512),
    actor_type VARCHAR(32),
    actor_id VARCHAR(64),
    worker_node VARCHAR(128),
    trace_id VARCHAR(128),
    dedupe_key VARCHAR(128),
    event_data TEXT,
    occurred_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_event_id ON tb_biz_task_event(event_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_task_event_dedupe ON tb_biz_task_event(task_id,dedupe_key);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_task ON tb_biz_task_event(task_id,occurred_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_execution ON tb_biz_task_event(execution_id,occurred_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_type ON tb_biz_task_event(event_type,occurred_at);

INSERT OR IGNORE INTO tb_menu(id,parent_id,menu_code,menu_name,menu_type,path,component,icon,sort_order,status,permission,meta) VALUES
(600,0,'TaskManagement','task.management.title',1,'/task','BasicLayout','lucide:list-checks',6,1,'','{"icon":"lucide:list-checks","title":"task.management.title","order":6}'),
(601,600,'TaskCenter','task.center.title',2,'/task/center','/task/center/list','lucide:activity',1,1,'Task:Query','{"icon":"lucide:activity","title":"task.center.title"}'),
(60101,601,'TaskQuery','task.action.query',3,NULL,NULL,'',1,1,'Task:Query','{"title":"task.action.query","hideInMenu":true}'),
(60102,601,'TaskControl','task.action.control',3,NULL,NULL,'',2,1,'Task:Control','{"title":"task.action.control","hideInMenu":true}');
INSERT OR IGNORE INTO tb_role_menu(role_id,menu_id)
SELECT 1,id FROM tb_menu WHERE id IN (600,601,60101,60102);

-- Destructive legacy removal is authorized and preflighted by
-- SqliteBusinessTaskSchemaMigrator before this idempotent no-op guard runs.
DROP TABLE IF EXISTS tb_export_task;
