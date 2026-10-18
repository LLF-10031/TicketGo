package com.ticketgo.product.service.impl;

import cn.hutool.core.util.IdUtil;
import com.ticketgo.common.exception.BizException;
import com.ticketgo.common.util.SnowflakeIdGenerator;
import com.ticketgo.product.dto.UploadResponse;
import com.ticketgo.product.entity.FileRecord;
import com.ticketgo.product.mapper.FileRecordMapper;
import com.ticketgo.product.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * 图片处理：校验 → 多规格压缩（原图/展示图 1200/缩略图 300）→ MinIO，失败降级本地。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;

    @Value("${ticketgo.minio.endpoint:http://127.0.0.1:9000}")
    private String endpoint;
    @Value("${ticketgo.minio.access-key:minioadmin}")
    private String accessKey;
    @Value("${ticketgo.minio.secret-key:minioadmin}")
    private String secretKey;
    @Value("${ticketgo.minio.bucket:ticketgo}")
    private String bucket;
    @Value("${ticketgo.minio.local-dir:./uploads}")
    private String localDir;

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> COMPRESSIBLE = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 10 * 1024 * 1024L;
    private static final int DISPLAY_WIDTH = 1200;
    private static final int THUMB_WIDTH = 300;

    @Override
    public UploadResponse upload(MultipartFile file, String bizType, Long bizId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("图片大小不能超过 10MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/gif 格式");
        }
        String biz = StringUtils.hasText(bizType) ? bizType : "poster";
        try {
            byte[] original = file.getBytes();
            byte[] display = compress(original, DISPLAY_WIDTH, ext);
            byte[] thumb = compress(original, THUMB_WIDTH, ext);
            String base = biz + "/" + LocalDate.now() + "/" + IdUtil.fastSimpleUUID();

            UploadResponse resp = store(base, ext, original, display, thumb);

            FileRecord record = new FileRecord();
            record.setId(SnowflakeIdGenerator.generateId());
            record.setBizType(biz);
            record.setBizId(bizId);
            record.setOriginalUrl(resp.originalUrl());
            record.setDisplayUrl(resp.displayUrl());
            record.setThumbUrl(resp.thumbUrl());
            record.setSize((int) file.getSize());
            record.setIsOrphan(1);
            record.setCreateTime(LocalDateTime.now());
            fileRecordMapper.insert(record);
            return resp;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BizException("图片上传失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(Long id) {
        fileRecordMapper.deleteById(id);
    }

    /** 等比缩放到最大宽度（Thumbnailator size 会保持宽高比并贴合边界） */
    private byte[] compress(byte[] src, int maxWidth, String ext) throws Exception {
        if (!COMPRESSIBLE.contains(ext)) {
            // webp 等标准 ImageIO 不支持的格式直接返回原图，保证不报错
            return src;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(src))
                .size(maxWidth, maxWidth)
                .outputQuality(0.85)
                .outputFormat("jpeg".equals(ext) ? "jpg" : ext)
                .toOutputStream(out);
        return out.toByteArray();
    }

    /** 先试 MinIO，失败降级本地磁盘 */
    private UploadResponse store(String base, String ext, byte[] original, byte[] display, byte[] thumb) {
        try {
            return storeMinio(base, ext, original, display, thumb);
        } catch (Exception e) {
            log.warn("MinIO 不可用，降级本地存储：{}", e.getMessage());
            return storeLocal(base, ext, original, display, thumb);
        }
    }

    private UploadResponse storeMinio(String base, String ext, byte[] original, byte[] display, byte[] thumb) throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        String originalObj = base + "." + ext;
        String displayObj = base + "_display." + ext;
        String thumbObj = base + "_thumb." + ext;
        put(client, originalObj, original, ext);
        put(client, displayObj, display, ext);
        put(client, thumbObj, thumb, ext);
        String prefix = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return new UploadResponse(prefix + bucket + "/" + originalObj,
                prefix + bucket + "/" + displayObj,
                prefix + bucket + "/" + thumbObj);
    }

    private void put(MinioClient client, String object, byte[] data, String ext) throws Exception {
        try (InputStream in = new ByteArrayInputStream(data)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .stream(in, data.length, -1)
                    .contentType("image/" + ("jpg".equals(ext) ? "jpeg" : ext))
                    .build());
        }
    }

    private UploadResponse storeLocal(String base, String ext, byte[] original, byte[] display, byte[] thumb) {
        try {
            String name = base.replace("/", "_");
            Path root = Paths.get(localDir).toAbsolutePath();
            Path originalDir = root.resolve("original");
            Path displayDir = root.resolve("display");
            Path thumbDir = root.resolve("thumb");
            Files.createDirectories(originalDir);
            Files.createDirectories(displayDir);
            Files.createDirectories(thumbDir);
            Files.write(originalDir.resolve(name + "." + ext), original);
            Files.write(displayDir.resolve(name + "." + ext), display);
            Files.write(thumbDir.resolve(name + "." + ext), thumb);
            return new UploadResponse("/uploads/original/" + name + "." + ext,
                    "/uploads/display/" + name + "." + ext,
                    "/uploads/thumb/" + name + "." + ext);
        } catch (Exception e) {
            throw new BizException("本地存储失败：" + e.getMessage());
        }
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
