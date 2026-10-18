package com.ticketgo.product.controller;

import com.ticketgo.common.result.Result;
import com.ticketgo.product.dto.UploadResponse;
import com.ticketgo.product.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（多规格压缩 + MinIO，失败降级本地）
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/api/file/upload")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(name = "biz_type", required = false) String bizType,
                                         @RequestParam(name = "biz_id", required = false) Long bizId) {
        return Result.ok(fileService.upload(file, bizType, bizId));
    }

    @DeleteMapping("/api/file/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }
}
