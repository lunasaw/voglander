# Voglander Logo 使用说明

## Logo 文件

本目录包含以下logo文件：

- `logo.svg` - 主要logo，尺寸 200x60px，适用于网页头部、文档等
- `favicon.svg` - 网站图标，尺寸 32x32px，用作浏览器标签页图标
- `index.html` - logo展示页面

## 设计说明

### 设计理念
- **科技蓝色主题**：使用渐变蓝色体现科技感和专业性
- **监控元素**：左侧圆形图案代表监控摄像头
- **现代风格**：采用扁平化设计和圆角元素

### 颜色方案
- 主色调：`#1e3a8a` 到 `#3b82f6` (深蓝到亮蓝渐变)
- 辅助色：`#0ea5e9` 到 `#06b6d4` (天蓝到青色渐变)
- 文字色：白色 `#ffffff`
- 副标题色：`#bfdbfe` (浅蓝色)

## 使用方式

### 在HTML中使用
```html
<!-- 作为网站favicon -->
<link rel="icon" type="image/svg+xml" href="/favicon.svg">

<!-- 作为页面logo -->
<img src="/logo.svg" alt="Voglander Logo" width="200" height="60">
```

### 在Spring Boot中配置
已在 `ResourcesConfig.java` 中配置了静态资源访问路径：
- `/favicon.ico` 和 `/favicon.svg` 映射到favicon
- `/static/**` 映射到静态资源目录

## 转换为其他格式

如需要其他格式（PNG、ICO等），可以使用以下工具：

### 使用ImageMagick转换
```bash
# 安装ImageMagick
brew install imagemagick  # macOS
sudo apt-get install imagemagick  # Ubuntu

# 转换为PNG
convert logo.svg -background transparent logo.png
convert favicon.svg -background transparent favicon.png

# 转换为ICO
convert favicon.svg -background transparent -define icon:auto-resize=16,32,48 favicon.ico
```

### 在线转换工具
- [CloudConvert](https://cloudconvert.com/svg-to-png)
- [Convertio](https://convertio.co/svg-png/)

## 自定义修改

logo使用SVG格式，可以轻松修改：
- 颜色：修改渐变色的 `stop-color` 值
- 文字：修改 `<text>` 元素的内容
- 尺寸：修改 `viewBox` 和 `width/height` 属性

## 预览

启动应用后，访问 `http://localhost:8080/index.html` 可以查看logo效果。