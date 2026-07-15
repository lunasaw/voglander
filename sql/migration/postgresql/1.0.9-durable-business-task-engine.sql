-- Voglander 1.0.9 durable business-task core migration (PostgreSQL)
-- Additive and repeatable. Legacy export removal is a separate guarded release action.

CREATE TABLE IF NOT EXISTS tb_biz_task
(
    id                   BIGSERIAL PRIMARY KEY,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    task_id              VARCHAR(64)  NOT NULL,
    task_type            VARCHAR(64)  NOT NULL,
    task_name            VARCHAR(255) NOT NULL,
    description          VARCHAR(512),
    task_mode            VARCHAR(32)  NOT NULL,
    schedule_start_time  TIMESTAMP,
    schedule_end_time    TIMESTAMP,
    interval_seconds     BIGINT,
    next_plan_time       TIMESTAMP,
    schedule_version     INTEGER      NOT NULL DEFAULT 1,
    state                VARCHAR(32)  NOT NULL,
    priority             INTEGER      NOT NULL DEFAULT 0,
    last_execution_id    VARCHAR(64),
    last_execute_time    TIMESTAMP,
    completed_time       TIMESTAMP,
    planned_count        INTEGER      NOT NULL DEFAULT 0,
    success_count        INTEGER      NOT NULL DEFAULT 0,
    failed_count         INTEGER      NOT NULL DEFAULT 0,
    missed_count         INTEGER      NOT NULL DEFAULT 0,
    cancelled_count      INTEGER      NOT NULL DEFAULT 0,
    progress_current     BIGINT       NOT NULL DEFAULT 0,
    progress_total       BIGINT       NOT NULL DEFAULT 0,
    progress_message     VARCHAR(512),
    progress_revision    BIGINT       NOT NULL DEFAULT 0,
    biz_key              VARCHAR(128),
    subject_type         VARCHAR(64),
    subject_id           VARCHAR(128),
    payload              TEXT         NOT NULL,
    payload_version      INTEGER      NOT NULL,
    result_ref_type      VARCHAR(64),
    result_ref_id        VARCHAR(128),
    result_summary       TEXT,
    last_failure_code    VARCHAR(64),
    last_failure_message VARCHAR(512),
    origin_task_id       VARCHAR(64),
    origin_execution_id  VARCHAR(64),
    owner_type           VARCHAR(32)  NOT NULL,
    owner_id             VARCHAR(64)  NOT NULL,
    organization_id      VARCHAR(64),
    idempotency_key      VARCHAR(128),
    version              INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uk_biz_task_task_id UNIQUE (task_id),
    CONSTRAINT uk_biz_task_idempotency UNIQUE (owner_type, owner_id, task_type, idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_biz_task_due ON tb_biz_task (state, next_plan_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_type_state ON tb_biz_task (task_type, state, create_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_owner ON tb_biz_task (owner_type, owner_id, create_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_biz_key ON tb_biz_task (biz_key, task_type);
CREATE INDEX IF NOT EXISTS idx_biz_task_subject ON tb_biz_task (subject_type, subject_id, create_time);

CREATE TABLE IF NOT EXISTS tb_biz_task_execution
(
    id                        BIGSERIAL PRIMARY KEY,
    create_time               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_id              VARCHAR(64)  NOT NULL,
    task_id                   VARCHAR(64)  NOT NULL,
    schedule_version          INTEGER      NOT NULL DEFAULT 1,
    planned_at                TIMESTAMP    NOT NULL,
    deadline_at               TIMESTAMP,
    state                     VARCHAR(32)  NOT NULL,
    attempt_count             INTEGER      NOT NULL DEFAULT 0,
    max_attempts              INTEGER      NOT NULL DEFAULT 1,
    next_attempt_time         TIMESTAMP,
    started_at                TIMESTAMP,
    heartbeat_at              TIMESTAMP,
    finished_at               TIMESTAMP,
    claim_token               VARCHAR(128),
    worker_node               VARCHAR(128),
    lease_until               TIMESTAMP,
    progress_current          BIGINT       NOT NULL DEFAULT 0,
    progress_total            BIGINT       NOT NULL DEFAULT 0,
    progress_message          VARCHAR(512),
    progress_revision         BIGINT       NOT NULL DEFAULT 0,
    result_ref_type           VARCHAR(64),
    result_ref_id             VARCHAR(128),
    result_summary            TEXT,
    failure_code              VARCHAR(64),
    failure_message           VARCHAR(512),
    retryable                 BOOLEAN      NOT NULL DEFAULT FALSE,
    retry_origin_execution_id VARCHAR(64),
    version                   INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uk_biz_task_execution_id UNIQUE (execution_id),
    CONSTRAINT uk_biz_task_execution_plan UNIQUE (task_id, schedule_version, planned_at)
);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_task ON tb_biz_task_execution (task_id, planned_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_pending ON tb_biz_task_execution (state, next_attempt_time);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_lease ON tb_biz_task_execution (state, lease_until);
CREATE INDEX IF NOT EXISTS idx_biz_task_execution_retry_origin ON tb_biz_task_execution (retry_origin_execution_id);

CREATE TABLE IF NOT EXISTS tb_biz_task_event
(
    id               BIGSERIAL PRIMARY KEY,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id         VARCHAR(64)  NOT NULL,
    task_id          VARCHAR(64)  NOT NULL,
    execution_id     VARCHAR(64),
    event_type       VARCHAR(64)  NOT NULL,
    from_state       VARCHAR(32),
    to_state         VARCHAR(32),
    attempt_no       INTEGER,
    progress_current BIGINT,
    progress_total   BIGINT,
    progress_message VARCHAR(512),
    failure_code     VARCHAR(64),
    failure_message  VARCHAR(512),
    actor_type       VARCHAR(32),
    actor_id         VARCHAR(64),
    worker_node      VARCHAR(128),
    trace_id         VARCHAR(128),
    dedupe_key       VARCHAR(128),
    event_data       TEXT,
    occurred_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_biz_task_event_id UNIQUE (event_id),
    CONSTRAINT uk_biz_task_event_dedupe UNIQUE (task_id, dedupe_key)
);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_task ON tb_biz_task_event (task_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_execution ON tb_biz_task_event (execution_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_biz_task_event_type ON tb_biz_task_event (event_type, occurred_at);

-- 任务中心菜单、按钮权限（Task:Query / Task:Control）
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
SELECT 600, 0, 'TaskManagement', 'task.management.title', 1, '/task', 'BasicLayout', 'lucide:list-checks', 6, 1, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE id = 600);
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
SELECT 601, 600, 'TaskCenter', 'task.center.title', 2, '/task/center', '/task/center/list', 'lucide:activity', 1,
       1, 'Task:Query', NULL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE id = 601);
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
SELECT 60101, 601, 'TaskQuery', 'task.action.query', 3, NULL, NULL, '', 1, 1, 'Task:Query', NULL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE id = 60101);
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
SELECT 60102, 601, 'TaskControl', 'task.action.control', 3, NULL, NULL, '', 2, 1, 'Task:Control', NULL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE id = 60102);

INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (600, 601, 60101, 60102)
  AND NOT EXISTS (SELECT 1 FROM tb_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = tb_menu.id);

-- The release preflight must create a native backup and set the destructive
-- authorization before this guarded, idempotent removal is applied.
DROP TABLE IF EXISTS tb_export_task;
