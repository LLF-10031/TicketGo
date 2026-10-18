package com.ticketgo.product.service;

import com.ticketgo.product.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    UploadResponse upload(MultipartFile file, String bizType, Long bizId);

    void delete(Long id);
}
