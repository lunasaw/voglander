-- Voglander 1.0.9 image asset collection migration (MySQL 8)
-- Non-destructive and repeatable. No DROP statements are permitted here.

CREATE TABLE IF NOT EXISTS `tb_image_asset` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `asset_id` VARCHAR(64) NOT NULL, `asset_name` VARCHAR(255) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE', `storage_provider` VARCHAR(32) NOT NULL,
    `storage_bucket` VARCHAR(128), `storage_key` VARCHAR(512) NOT NULL, `storage_node_id` VARCHAR(128),
    `content_type` VARCHAR(64) NOT NULL, `image_format` VARCHAR(32) NOT NULL,
    `file_size` BIGINT NOT NULL, `width` INT NOT NULL, `height` INT NOT NULL,
    `checksum_algorithm` VARCHAR(32) NOT NULL DEFAULT 'SHA256', `checksum` VARCHAR(128) NOT NULL,
    `captured_at` DATETIME NOT NULL, `ingested_at` DATETIME NOT NULL,
    `owner_type` VARCHAR(32) NOT NULL, `owner_id` VARCHAR(64) NOT NULL,
    `organization_id` VARCHAR(64), `idempotency_key` VARCHAR(128),
    `retention_policy` VARCHAR(64) NOT NULL DEFAULT 'PERMANENT', `expires_at` DATETIME,
    `deleted_at` DATETIME, `delete_reason` VARCHAR(255), `failure_code` VARCHAR(64),
    `failure_message` VARCHAR(512), `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_image_asset_asset_id` (`asset_id`),
    UNIQUE KEY `uk_image_asset_idempotency` (`owner_type`,`owner_id`,`idempotency_key`),
    KEY `idx_image_asset_status_created` (`status`,`create_time`),
    KEY `idx_image_asset_captured` (`captured_at`,`asset_id`),
    KEY `idx_image_asset_owner` (`owner_type`,`owner_id`,`create_time`),
    KEY `idx_image_asset_checksum` (`checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `tb_image_asset_source` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `asset_id` VARCHAR(64) NOT NULL, `source_type` VARCHAR(32) NOT NULL,
    `source_system` VARCHAR(64) NOT NULL, `source_entity_type` VARCHAR(32) NOT NULL,
    `source_entity_id` VARCHAR(128) NOT NULL, `source_task_id` VARCHAR(64),
    `source_execution_id` VARCHAR(64), `original_filename` VARCHAR(255), `source_metadata` TEXT,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_image_asset_source_asset` (`asset_id`),
    UNIQUE KEY `uk_image_asset_source_execution` (`source_execution_id`),
    KEY `idx_image_asset_source_type_created` (`source_type`,`create_time`),
    KEY `idx_image_asset_source_entity_created` (`source_entity_type`,`source_entity_id`,`create_time`),
    KEY `idx_image_asset_source_task` (`source_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `tb_image_collection_config` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `task_id` VARCHAR(64) NOT NULL, `device_id` VARCHAR(64) NOT NULL, `channel_id` VARCHAR(64) NOT NULL,
    `device_name_snapshot` VARCHAR(255), `channel_name_snapshot` VARCHAR(255),
    `retention_policy` VARCHAR(64) NOT NULL DEFAULT 'PERMANENT', `capture_options` TEXT,
    `version` INT NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
    UNIQUE KEY `uk_image_collection_config_task` (`task_id`),
    KEY `idx_image_collection_config_camera` (`device_id`,`channel_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT INTO `tb_menu` (`id`,`parent_id`,`menu_code`,`menu_name`,`menu_type`,`path`,`component`,`icon`,`sort_order`,`status`,`permission`,`meta`) VALUES
(700,0,'ImageManagement','image.management.title',1,'/image','BasicLayout','lucide:images',7,1,'',JSON_OBJECT('icon','lucide:images','title','image.management.title','order',7)),
(701,700,'ImageAssets','image.asset.title',2,'/image/assets','/image/assets/list','lucide:image',1,1,'Image:Asset:Query',JSON_OBJECT('icon','lucide:image','title','image.asset.title')),
(702,700,'ImageCollection','image.collection.title',2,'/image/collection','/image/collection/list','lucide:camera',2,1,'Image:Collection:Query',JSON_OBJECT('icon','lucide:camera','title','image.collection.title')),
(70101,701,'ImageAssetQuery','image.asset.query',3,NULL,NULL,'',1,1,'Image:Asset:Query',JSON_OBJECT('title','image.asset.query','hideInMenu',true)),
(70102,701,'ImageAssetView','image.asset.view',3,NULL,NULL,'',2,1,'Image:Asset:View',JSON_OBJECT('title','image.asset.view','hideInMenu',true)),
(70103,701,'ImageAssetUpload','image.asset.upload',3,NULL,NULL,'',3,1,'Image:Asset:Upload',JSON_OBJECT('title','image.asset.upload','hideInMenu',true)),
(70104,701,'ImageAssetDownload','image.asset.download',3,NULL,NULL,'',4,1,'Image:Asset:Download',JSON_OBJECT('title','image.asset.download','hideInMenu',true)),
(70105,701,'ImageAssetDelete','image.asset.delete',3,NULL,NULL,'',5,1,'Image:Asset:Delete',JSON_OBJECT('title','image.asset.delete','hideInMenu',true)),
(70201,702,'ImageCollectionQuery','image.collection.query',3,NULL,NULL,'',1,1,'Image:Collection:Query',JSON_OBJECT('title','image.collection.query','hideInMenu',true)),
(70202,702,'ImageCollectionCreate','image.collection.create',3,NULL,NULL,'',2,1,'Image:Collection:Create',JSON_OBJECT('title','image.collection.create','hideInMenu',true)),
(70203,702,'ImageCollectionControl','image.collection.control',3,NULL,NULL,'',3,1,'Image:Collection:Control',JSON_OBJECT('title','image.collection.control','hideInMenu',true))
ON DUPLICATE KEY UPDATE `parent_id`=VALUES(`parent_id`),`menu_name`=VALUES(`menu_name`),
`path`=VALUES(`path`),`component`=VALUES(`component`),`permission`=VALUES(`permission`),`meta`=VALUES(`meta`);

INSERT IGNORE INTO `tb_role_menu`(`role_id`,`menu_id`)
SELECT 1,`id` FROM `tb_menu`
WHERE `id` IN (700,701,702,70101,70102,70103,70104,70105,70201,70202,70203);

-- Verification queries
SELECT table_name FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_name LIKE 'tb_image_%' ORDER BY table_name;
SHOW INDEX FROM tb_image_collection_config;
SELECT id,parent_id,permission FROM tb_menu WHERE id BETWEEN 700 AND 70203 ORDER BY id;
