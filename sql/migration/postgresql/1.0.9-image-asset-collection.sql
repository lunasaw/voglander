-- Voglander 1.0.9 image asset collection migration (PostgreSQL)
-- Non-destructive and repeatable. No MySQL or SQLite syntax is used.

CREATE TABLE IF NOT EXISTS tb_image_asset (
    id BIGSERIAL PRIMARY KEY, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, asset_id VARCHAR(64) NOT NULL,
    asset_name VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    storage_provider VARCHAR(32) NOT NULL, storage_bucket VARCHAR(128), storage_key VARCHAR(512) NOT NULL,
    storage_node_id VARCHAR(128), content_type VARCHAR(64) NOT NULL, image_format VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL,
    checksum_algorithm VARCHAR(32) NOT NULL DEFAULT 'SHA256', checksum VARCHAR(128) NOT NULL,
    captured_at TIMESTAMP NOT NULL, ingested_at TIMESTAMP NOT NULL, owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL, organization_id VARCHAR(64), idempotency_key VARCHAR(128),
    retention_policy VARCHAR(64) NOT NULL DEFAULT 'PERMANENT', expires_at TIMESTAMP, deleted_at TIMESTAMP,
    delete_reason VARCHAR(255), failure_code VARCHAR(64), failure_message VARCHAR(512), version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_image_asset_asset_id UNIQUE(asset_id),
    CONSTRAINT uk_image_asset_idempotency UNIQUE(owner_type,owner_id,idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_image_asset_status_created ON tb_image_asset(status,create_time);
CREATE INDEX IF NOT EXISTS idx_image_asset_captured ON tb_image_asset(captured_at,asset_id);
CREATE INDEX IF NOT EXISTS idx_image_asset_owner ON tb_image_asset(owner_type,owner_id,create_time);
CREATE INDEX IF NOT EXISTS idx_image_asset_checksum ON tb_image_asset(checksum);

CREATE TABLE IF NOT EXISTS tb_image_asset_source (
    id BIGSERIAL PRIMARY KEY, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asset_id VARCHAR(64) NOT NULL, source_type VARCHAR(32) NOT NULL, source_system VARCHAR(64) NOT NULL,
    source_entity_type VARCHAR(32) NOT NULL, source_entity_id VARCHAR(128) NOT NULL,
    source_task_id VARCHAR(64), source_execution_id VARCHAR(64), original_filename VARCHAR(255), source_metadata TEXT,
    CONSTRAINT uk_image_asset_source_asset UNIQUE(asset_id),
    CONSTRAINT uk_image_asset_source_execution UNIQUE(source_execution_id)
);
CREATE INDEX IF NOT EXISTS idx_image_asset_source_type_created ON tb_image_asset_source(source_type,create_time);
CREATE INDEX IF NOT EXISTS idx_image_asset_source_entity_created ON tb_image_asset_source(source_entity_type,source_entity_id,create_time);
CREATE INDEX IF NOT EXISTS idx_image_asset_source_task ON tb_image_asset_source(source_task_id);

CREATE TABLE IF NOT EXISTS tb_image_collection_config (
    id BIGSERIAL PRIMARY KEY, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, task_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(64) NOT NULL, channel_id VARCHAR(64) NOT NULL,
    device_name_snapshot VARCHAR(255), channel_name_snapshot VARCHAR(255),
    retention_policy VARCHAR(64) NOT NULL DEFAULT 'PERMANENT', capture_options TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_image_collection_config_task UNIQUE(task_id)
);
CREATE INDEX IF NOT EXISTS idx_image_collection_config_camera ON tb_image_collection_config(device_id,channel_id,create_time);

INSERT INTO tb_menu(id,parent_id,menu_code,menu_name,menu_type,path,component,icon,sort_order,status,permission,meta) VALUES
(700,0,'ImageManagement','image.management.title',1,'/image','BasicLayout','lucide:images',7,1,'','{"icon":"lucide:images","title":"image.management.title","order":7}'::json),
(701,700,'ImageAssets','image.asset.title',2,'/image/assets','/image/assets/list','lucide:image',1,1,'Image:Asset:Query','{"icon":"lucide:image","title":"image.asset.title"}'::json),
(702,700,'ImageCollection','image.collection.title',2,'/image/collection','/image/collection/list','lucide:camera',2,1,'Image:Collection:Query','{"icon":"lucide:camera","title":"image.collection.title"}'::json),
(70101,701,'ImageAssetQuery','image.asset.query',3,NULL,NULL,'',1,1,'Image:Asset:Query','{"title":"image.asset.query","hideInMenu":true}'::json),
(70102,701,'ImageAssetView','image.asset.view',3,NULL,NULL,'',2,1,'Image:Asset:View','{"title":"image.asset.view","hideInMenu":true}'::json),
(70103,701,'ImageAssetUpload','image.asset.upload',3,NULL,NULL,'',3,1,'Image:Asset:Upload','{"title":"image.asset.upload","hideInMenu":true}'::json),
(70104,701,'ImageAssetDownload','image.asset.download',3,NULL,NULL,'',4,1,'Image:Asset:Download','{"title":"image.asset.download","hideInMenu":true}'::json),
(70105,701,'ImageAssetDelete','image.asset.delete',3,NULL,NULL,'',5,1,'Image:Asset:Delete','{"title":"image.asset.delete","hideInMenu":true}'::json),
(70201,702,'ImageCollectionQuery','image.collection.query',3,NULL,NULL,'',1,1,'Image:Collection:Query','{"title":"image.collection.query","hideInMenu":true}'::json),
(70202,702,'ImageCollectionCreate','image.collection.create',3,NULL,NULL,'',2,1,'Image:Collection:Create','{"title":"image.collection.create","hideInMenu":true}'::json),
(70203,702,'ImageCollectionControl','image.collection.control',3,NULL,NULL,'',3,1,'Image:Collection:Control','{"title":"image.collection.control","hideInMenu":true}'::json)
ON CONFLICT(id) DO UPDATE SET parent_id=EXCLUDED.parent_id,menu_name=EXCLUDED.menu_name,path=EXCLUDED.path,
component=EXCLUDED.component,permission=EXCLUDED.permission,meta=EXCLUDED.meta;

INSERT INTO tb_role_menu(role_id,menu_id)
SELECT 1,id FROM tb_menu WHERE id IN (700,701,702,70101,70102,70103,70104,70105,70201,70202,70203)
ON CONFLICT(role_id,menu_id) DO NOTHING;

-- Verification queries (psql)
SELECT tablename FROM pg_tables WHERE schemaname=current_schema() AND tablename LIKE 'tb_image_%' ORDER BY tablename;
SELECT indexname,indexdef FROM pg_indexes WHERE schemaname=current_schema() AND tablename LIKE 'tb_image_%' ORDER BY indexname;
SELECT id,parent_id,permission FROM tb_menu WHERE id BETWEEN 700 AND 70203 ORDER BY id;
