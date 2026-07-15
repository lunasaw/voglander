-- Voglander 1.0.9 durable business-task core migration (MySQL 8)
-- Additive and repeatable. Legacy export removal is a separate guarded release action.

CREATE TABLE IF NOT EXISTS `tb_biz_task`
(
    `id`                   bigint unsigned NOT NULL AUTO_INCREMENT,
    `create_time`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `task_id`              varchar(64)     NOT NULL,
    `task_type`            varchar(64)     NOT NULL,
    `task_name`            varchar(255)    NOT NULL,
    `description`          varchar(512)             DEFAULT NULL,
    `task_mode`            varchar(32)     NOT NULL,
    `schedule_start_time`  datetime                 DEFAULT NULL,
    `schedule_end_time`    datetime                 DEFAULT NULL,
    `interval_seconds`     bigint                   DEFAULT NULL,
    `next_plan_time`       datetime                 DEFAULT NULL,
    `schedule_version`     int             NOT NULL DEFAULT 1,
    `state`                varchar(32)     NOT NULL,
    `priority`             int             NOT NULL DEFAULT 0,
    `last_execution_id`    varchar(64)              DEFAULT NULL,
    `last_execute_time`    datetime                 DEFAULT NULL,
    `completed_time`       datetime                 DEFAULT NULL,
    `planned_count`        int             NOT NULL DEFAULT 0,
    `success_count`        int             NOT NULL DEFAULT 0,
    `failed_count`         int             NOT NULL DEFAULT 0,
    `missed_count`         int             NOT NULL DEFAULT 0,
    `cancelled_count`      int             NOT NULL DEFAULT 0,
    `progress_current`     bigint          NOT NULL DEFAULT 0,
    `progress_total`       bigint          NOT NULL DEFAULT 0,
    `progress_message`     varchar(512)             DEFAULT NULL,
    `progress_revision`    bigint          NOT NULL DEFAULT 0,
    `biz_key`              varchar(128)             DEFAULT NULL,
    `subject_type`         varchar(64)              DEFAULT NULL,
    `subject_id`           varchar(128)             DEFAULT NULL,
    `payload`              text            NOT NULL,
    `payload_version`      int             NOT NULL,
    `result_ref_type`      varchar(64)              DEFAULT NULL,
    `result_ref_id`        varchar(128)             DEFAULT NULL,
    `result_summary`       text,
    `last_failure_code`    varchar(64)              DEFAULT NULL,
    `last_failure_message` varchar(512)             DEFAULT NULL,
    `origin_task_id`       varchar(64)              DEFAULT NULL,
    `origin_execution_id`  varchar(64)              DEFAULT NULL,
    `owner_type`           varchar(32)     NOT NULL,
    `owner_id`             varchar(64)     NOT NULL,
    `organization_id`      varchar(64)              DEFAULT NULL,
    `idempotency_key`      varchar(128)             DEFAULT NULL,
    `version`              int             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_task_task_id` (`task_id`),
    UNIQUE KEY `uk_biz_task_idempotency` (`owner_type`, `owner_id`, `task_type`, `idempotency_key`),
    KEY `idx_biz_task_due` (`state`, `next_plan_time`),
    KEY `idx_biz_task_type_state` (`task_type`, `state`, `create_time`),
    KEY `idx_biz_task_owner` (`owner_type`, `owner_id`, `create_time`),
    KEY `idx_biz_task_biz_key` (`biz_key`, `task_type`),
    KEY `idx_biz_task_subject` (`subject_type`, `subject_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '通用业务任务';

CREATE TABLE IF NOT EXISTS `tb_biz_task_execution`
(
    `id`                        bigint unsigned NOT NULL AUTO_INCREMENT,
    `create_time`               datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`               datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `execution_id`              varchar(64)     NOT NULL,
    `task_id`                   varchar(64)     NOT NULL,
    `schedule_version`          int             NOT NULL DEFAULT 1,
    `planned_at`                datetime        NOT NULL,
    `deadline_at`               datetime                 DEFAULT NULL,
    `state`                     varchar(32)     NOT NULL,
    `attempt_count`             int             NOT NULL DEFAULT 0,
    `max_attempts`              int             NOT NULL DEFAULT 1,
    `next_attempt_time`         datetime                 DEFAULT NULL,
    `started_at`                datetime                 DEFAULT NULL,
    `heartbeat_at`              datetime                 DEFAULT NULL,
    `finished_at`               datetime                 DEFAULT NULL,
    `claim_token`               varchar(128)             DEFAULT NULL,
    `worker_node`               varchar(128)             DEFAULT NULL,
    `lease_until`               datetime                 DEFAULT NULL,
    `progress_current`          bigint          NOT NULL DEFAULT 0,
    `progress_total`            bigint          NOT NULL DEFAULT 0,
    `progress_message`          varchar(512)             DEFAULT NULL,
    `progress_revision`         bigint          NOT NULL DEFAULT 0,
    `result_ref_type`           varchar(64)              DEFAULT NULL,
    `result_ref_id`             varchar(128)             DEFAULT NULL,
    `result_summary`            text,
    `failure_code`              varchar(64)              DEFAULT NULL,
    `failure_message`           varchar(512)             DEFAULT NULL,
    `retryable`                 tinyint(1)      NOT NULL DEFAULT 0,
    `retry_origin_execution_id` varchar(64)              DEFAULT NULL,
    `version`                   int             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_task_execution_id` (`execution_id`),
    UNIQUE KEY `uk_biz_task_execution_plan` (`task_id`, `schedule_version`, `planned_at`),
    KEY `idx_biz_task_execution_task` (`task_id`, `planned_at`),
    KEY `idx_biz_task_execution_pending` (`state`, `next_attempt_time`),
    KEY `idx_biz_task_execution_lease` (`state`, `lease_until`),
    KEY `idx_biz_task_execution_retry_origin` (`retry_origin_execution_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '通用业务任务执行';

CREATE TABLE IF NOT EXISTS `tb_biz_task_event`
(
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT,
    `create_time`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `event_id`         varchar(64)     NOT NULL,
    `task_id`          varchar(64)     NOT NULL,
    `execution_id`     varchar(64)              DEFAULT NULL,
    `event_type`       varchar(64)     NOT NULL,
    `from_state`       varchar(32)              DEFAULT NULL,
    `to_state`         varchar(32)              DEFAULT NULL,
    `attempt_no`       int                      DEFAULT NULL,
    `progress_current` bigint                   DEFAULT NULL,
    `progress_total`   bigint                   DEFAULT NULL,
    `progress_message` varchar(512)             DEFAULT NULL,
    `failure_code`     varchar(64)              DEFAULT NULL,
    `failure_message`  varchar(512)             DEFAULT NULL,
    `actor_type`       varchar(32)              DEFAULT NULL,
    `actor_id`         varchar(64)              DEFAULT NULL,
    `worker_node`      varchar(128)             DEFAULT NULL,
    `trace_id`         varchar(128)             DEFAULT NULL,
    `dedupe_key`       varchar(128)             DEFAULT NULL,
    `event_data`       text,
    `occurred_at`      datetime        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_task_event_id` (`event_id`),
    UNIQUE KEY `uk_biz_task_event_dedupe` (`task_id`, `dedupe_key`),
    KEY `idx_biz_task_event_task` (`task_id`, `occurred_at`),
    KEY `idx_biz_task_event_execution` (`execution_id`, `occurred_at`),
    KEY `idx_biz_task_event_type` (`event_type`, `occurred_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '通用业务任务事件';

-- 任务中心菜单、按钮权限（Task:Query / Task:Control）
INSERT INTO `tb_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`,
                       `sort_order`, `status`, `permission`, `meta`)
VALUES (600, 0, 'TaskManagement', 'task.management.title', 1, '/task', 'BasicLayout', 'lucide:list-checks', 6, 1, '',
        JSON_OBJECT('icon', 'lucide:list-checks', 'title', 'task.management.title', 'order', 6)),
       (601, 600, 'TaskCenter', 'task.center.title', 2, '/task/center', '/task/center/list', 'lucide:activity', 1,
        1, 'Task:Query', JSON_OBJECT('icon', 'lucide:activity', 'title', 'task.center.title')),
       (60101, 601, 'TaskQuery', 'task.action.query', 3, NULL, NULL, '', 1, 1, 'Task:Query',
        JSON_OBJECT('title', 'task.action.query', 'hideInMenu', true)),
       (60102, 601, 'TaskControl', 'task.action.control', 3, NULL, NULL, '', 2, 1, 'Task:Control',
        JSON_OBJECT('title', 'task.action.control', 'hideInMenu', true))
ON DUPLICATE KEY UPDATE `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`),
                        `path` = VALUES(`path`), `component` = VALUES(`component`),
                        `permission` = VALUES(`permission`), `meta` = VALUES(`meta`);

INSERT IGNORE INTO `tb_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id`
FROM `tb_menu`
WHERE `id` IN (600, 601, 60101, 60102);

-- The release preflight must create a native backup and set the destructive
-- authorization before this guarded, idempotent removal is applied.
DROP TABLE IF EXISTS `tb_export_task`;
