package com.sagares.saga_res_api.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public UploadResult uploadDishImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "saga-res/dishes",
                            "resource_type", "image"));

            return new UploadResult(
                    result.get("secure_url").toString(),
                    result.get("public_id").toString()
            );

        } catch (IOException e) {
            throw new RuntimeException("Tải hình ảnh thất bại", e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu hình ảnh");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là hình ảnh");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Kích thước file phải nhỏ hơn 5MB"
            );
        }
    }

    public record UploadResult(
            String url,
            String publicId
    ) {}
}