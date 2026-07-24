package io.github.lunasaw.voglander.web.api.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.UserDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.service.image.ImageAssetLifecycleService;
import io.github.lunasaw.voglander.service.image.ImageAssetReadService;
import io.github.lunasaw.voglander.service.image.ImageThumbnailResult;
import io.github.lunasaw.voglander.service.image.ImageThumbnailService;
import io.github.lunasaw.voglander.service.image.ImageIngestCommand;
import io.github.lunasaw.voglander.service.image.ImageIngestService;
import io.github.lunasaw.voglander.service.task.BusinessTaskAuditService;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;
import io.github.lunasaw.voglander.web.api.image.req.ImageAssetDeleteReq;
import io.github.lunasaw.voglander.web.api.image.req.ImageAssetQueryReq;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetConstraintsVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetStatisticsVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping(ApiConstant.API_INDEX_V1 + "/images")
@Tag(name = "图像资产")
public class ImageAssetController {
    private final ImageActorResolver actorResolver;
    private final ImageAssetManager assetManager;
    private final ImageAssetWebAssembler assembler;
    private final ImageIngestService ingestService;
    private final ImageAssetLifecycleService lifecycleService;
    private final ImageStorageService storage;
    private final io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties properties;
    private final ImageAssetReadService readService;
    private final ImageThumbnailService thumbnailService;
    @Autowired(required = false)
    private BusinessTaskAuditService auditService;

    public ImageAssetController(ImageActorResolver actorResolver, ImageAssetManager assetManager,
        ImageAssetWebAssembler assembler, ImageIngestService ingestService, ImageAssetLifecycleService lifecycleService,
        ImageStorageService storage, io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties properties) {
        this(actorResolver, assetManager, assembler, ingestService, lifecycleService, storage, properties,
            new ImageAssetReadService(assetManager, storage), null);
    }

    @Autowired
    public ImageAssetController(ImageActorResolver actorResolver, ImageAssetManager assetManager,
        ImageAssetWebAssembler assembler, ImageIngestService ingestService, ImageAssetLifecycleService lifecycleService,
        ImageStorageService storage, io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties properties,
        ImageAssetReadService readService, ImageThumbnailService thumbnailService) {
        this.actorResolver = actorResolver; this.assetManager = assetManager; this.assembler = assembler;
        this.ingestService = ingestService; this.lifecycleService = lifecycleService; this.storage = storage; this.properties = properties;
        this.readService = readService; this.thumbnailService = thumbnailService;
    }

    @GetMapping("/constraints")
    @Operation(summary = "图像约束")
    public AjaxResult<ImageAssetConstraintsVO> constraints(@RequestHeader("Authorization") String authorization) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_QUERY);
        ImageAssetConstraintsVO vo = new ImageAssetConstraintsVO(); vo.setMaxUploadBytes(properties.getStorage().getMaxUploadBytes());
        vo.setMaxPixels(properties.getCollection().getMaxPixels()); vo.setMaxPlannedCount(properties.getCollection().getMaxPlannedCount());
        vo.setMinIntervalSeconds(properties.getCollection().getMinIntervalSeconds()); vo.setFormats(new String[] {"JPEG", "PNG", "WEBP"});
        return AjaxResult.success(vo);
    }

    @GetMapping("/statistics")
    @Operation(summary = "图像资产统计")
    public AjaxResult<ImageAssetStatisticsVO> statistics(@RequestHeader("Authorization") String authorization) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_QUERY);
        return AjaxResult.success(assembler.toVO(assetManager.statistics(null, null)));
    }

    @PostMapping("/getPage")
    @Operation(summary = "图像资产分页")
    public AjaxResult<java.util.Map<String, Object>> page(@RequestHeader("Authorization") String authorization,
        @RequestBody(required = false) ImageAssetQueryReq request, @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "24") long size) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_QUERY);
        ImageAssetQueryDTO query = assembler.toQuery(request);
        Page<ImageAssetEnrichedDTO> source = assetManager.getEnrichedPage(query, page, size);
        List<ImageAssetVO> records = new ArrayList<>();
        for (ImageAssetEnrichedDTO item : source.getRecords()) records.add(assembler.toVO(item.getAsset(), item.getSource()));
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>(); response.put("total", source.getTotal()); response.put("items", records);
        return AjaxResult.success(response);
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "图像资产详情")
    public AjaxResult<ImageAssetVO> detail(@RequestHeader("Authorization") String authorization, @PathVariable String assetId) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_QUERY);
        actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_VIEW);
        ImageAssetEnrichedDTO item = assetManager.getEnrichedDetail(assetId);
        if (item == null || item.getAsset() == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND);
        return AjaxResult.success(assembler.toVO(item.getAsset(), item.getSource()));
    }

    @PostMapping("/uploads")
    @Operation(summary = "上传图像资产")
    public AjaxResult<ImageAssetVO> upload(@RequestHeader("Authorization") String authorization,
        @Parameter(required = false, description = "可选兼容幂等键，1-128 个可见 ASCII 字符；图像 UI 调用必须提供")
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestParam("file") MultipartFile file, @RequestParam(value = "assetName", required = false) String assetName) throws IOException {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_UPLOAD);
        ImageAssetDTO asset = ingestService.ingestUpload(new ImageIngestCommand("USER", String.valueOf(actor.getId()), null,
            idempotencyKey, file.getOriginalFilename(), file.getContentType(), assetName), file.getInputStream());
        return AjaxResult.success(assembler.toVO(asset, assetManager.getSourceByAssetId(asset.getAssetId())));
    }

    @DeleteMapping("/{assetId}")
    @Operation(summary = "删除图像资产")
    public AjaxResult<Boolean> delete(@RequestHeader("Authorization") String authorization, @PathVariable String assetId,
        @RequestBody(required = false) ImageAssetDeleteReq ignored) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_DELETE);
        boolean accepted = lifecycleService.delete(assetId, null, null);
        audit(assetId, "ASSET_DELETE", accepted ? "OK" : "NOT_FOUND", actor);
        return AjaxResult.success(accepted);
    }

    @PostMapping("/{assetId}/delete:retry")
    @Operation(summary = "重试删除图像资产")
    public AjaxResult<Boolean> retryDelete(@RequestHeader("Authorization") String authorization, @PathVariable String assetId) {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_DELETE);
        boolean accepted = lifecycleService.retryDelete(assetId, null, null);
        audit(assetId, "ASSET_DELETE_RETRY", accepted ? "OK" : "NOT_FOUND", actor);
        return AjaxResult.success(accepted);
    }

    @GetMapping("/{assetId}/content")
    @Operation(summary = "预览图像资产", description = "仅返回已验证的图像内容，支持 ETag/304，响应不暴露存储内部字段")
    public ResponseEntity<StreamingResponseBody> content(@RequestHeader("Authorization") String authorization,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch, @PathVariable String assetId) throws IOException {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_VIEW);
        return stream(assetId, ifNoneMatch, false);
    }

    @GetMapping("/{assetId}/thumbnail")
    @Operation(summary = "获取私有缩略图", description = "只接受 table/gallery 固定规格，支持私有缓存 ETag/304")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "缩略图内容",
            content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "304", description = "ETag 命中，无响应体"),
        @ApiResponse(responseCode = "400", description = "profile 或请求参数无效",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "401", description = "未登录或 token 无效",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "403", description = "缺少 Image:Asset:View 权限",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "404", description = "资产不存在",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "409", description = "资产状态不允许读取",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "410", description = "资产已删除",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class))),
        @ApiResponse(responseCode = "503", description = "存储或缩略图派生暂不可用",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AjaxResult.class)))
    })
    public ResponseEntity<byte[]> thumbnail(@RequestHeader("Authorization") String authorization,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
        @PathVariable String assetId,
        @Parameter(description = "固定缩略图规格", required = true,
            schema = @Schema(allowableValues = {"table", "gallery"})) @RequestParam String profile) {
        UserDTO actor = actorResolver.resolve(authorization);
        actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_VIEW);
        ImageThumbnailResult result = thumbnailService.get(assetId, profile, ifNoneMatch);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(
                result.isNotModified() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK)
            .eTag(result.getEtag())
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
            .header(HttpHeaders.VARY, HttpHeaders.AUTHORIZATION)
            .header("X-Content-Type-Options", "nosniff");
        if (result.isNotModified()) return response.build();
        byte[] content = result.getContent();
        return response.contentType(MediaType.IMAGE_JPEG)
            .contentLength(content.length)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
            .body(content);
    }

    private ResponseEntity<StreamingResponseBody> stream(String assetId, String ifNoneMatch, boolean download) throws IOException {
        ImageAssetDTO asset = readService.requireReadable(assetId);
        String etag = "\"sha256:" + asset.getChecksum() + "\""; if (etag.equals(ifNoneMatch)) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        ImageContent content = readService.open(asset);
        StreamingResponseBody body = output -> { try (ImageContent ignored = content) { content.inputStream().transferTo(output); } };
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().eTag(etag)
            .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePrivate())
            .contentType(MediaType.parseMediaType(asset.getContentType())).contentLength(content.contentLength())
            .header("X-Content-Type-Options", "nosniff");
        if (download) {
            io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO source = assetManager.getSourceByAssetId(assetId);
            String filename = source == null ? asset.getAssetName() : source.getOriginalFilename();
            filename = safeDownloadFilename(filename, asset.getAssetId() + "." + asset.getImageFormat().toLowerCase());
            response.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
        } else {
            response.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString());
        }
        return response.body(body);
    }

    @GetMapping("/{assetId}/download")
    @Operation(summary = "下载图像资产", description = "以 RFC 5987 文件名下载已授权图像内容")
    public ResponseEntity<StreamingResponseBody> download(@RequestHeader("Authorization") String authorization, @PathVariable String assetId) throws IOException {
        UserDTO actor = actorResolver.resolve(authorization); actorResolver.require(actor, ImageConstant.PERMISSION_ASSET_VIEW);
        return stream(assetId, null, true);
    }

    private static String safeDownloadFilename(String filename, String fallback) {
        String safe = filename == null ? "" : filename.replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        if (slash >= 0) safe = safe.substring(slash + 1);
        safe = safe.replaceAll("[\\r\\n\\t\\p{Cntrl}]", "").trim();
        if (safe.isEmpty()) safe = fallback;
        return safe.length() > 255 ? safe.substring(0, 255) : safe;
    }

    private void audit(String assetId, String command, String resultCode, UserDTO actor) {
        if (auditService == null) return;
        auditService.record(new io.github.lunasaw.voglander.common.event.BusinessTaskAuditRecord(true, null,
            "USER", actor == null || actor.getId() == null ? null : actor.getId().toString(), assetId, null,
            command, null, null, resultCode, System.currentTimeMillis()));
    }
}
