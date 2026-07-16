# 图片管理模块国际化翻译

## 需要添加到前端 i18n 文件的翻译

### 中文 (zh-CN.json)

```json
{
  "image": {
    "management": {
      "title": "图片管理"
    },
    "asset": {
      "title": "图片资产",
      "query": "查询图片",
      "view": "查看图片",
      "upload": "上传图片",
      "download": "下载图片",
      "delete": "删除图片"
    },
    "collection": {
      "title": "图片采集",
      "query": "查询采集任务",
      "create": "创建采集任务",
      "control": "控制采集"
    }
  }
}
```

### 英文 (en-US.json)

```json
{
  "image": {
    "management": {
      "title": "Image Management"
    },
    "asset": {
      "title": "Image Assets",
      "query": "Query Images",
      "view": "View Image",
      "upload": "Upload Image",
      "download": "Download Image",
      "delete": "Delete Image"
    },
    "collection": {
      "title": "Image Collection",
      "query": "Query Collection Tasks",
      "create": "Create Collection Task",
      "control": "Control Collection"
    }
  }
}
```

## 使用位置

这些翻译 key 在以下数据库菜单中使用：

- **菜单 700**: ImageManagement - `image.management.title`
- **菜单 701**: ImageAssets - `image.asset.title`
- **菜单 702**: ImageCollection - `image.collection.title`
- **按钮权限**: 
  - 70101~70105: image.asset.* (查询、查看、上传、下载、删除)
  - 70201~70203: image.collection.* (查询、创建、控制)

## 前端集成说明

1. 将上述 JSON 内容合并到前端项目的国际化文件中
2. 通常路径为：
   - `src/locales/zh-CN.json` 或 `src/locales/lang/zh-CN/**.json`
   - `src/locales/en-US.json` 或 `src/locales/lang/en-US/**.json`
3. 确保前端菜单组件使用 `$t('image.asset.title')` 等方式读取翻译
