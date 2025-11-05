package cn.lzx.blog.service.impl;

import cn.lzx.blog.integration.storage.MinioUtil;
import cn.lzx.blog.service.FileUploadService;
import cn.lzx.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final MinioUtil minioUtil;

    // 支持的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/jpg"
    );

    // 头像大小限制 2MB
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;

    // 文章封面大小限制 5MB
    private static final long COVER_MAX_SIZE = 5 * 1024 * 1024;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        // 1. 校验文件
        validateFile(file, AVATAR_MAX_SIZE);

        try {
            // 2. 生成固定文件名：avatars/user_{userId}_avatar.{ext}
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = String.format("avatars/user_%d_avatar%s", userId, fileExtension);

            // 3. 删除旧头像（如果存在不同格式的旧文件）
            deleteOldAvatars(userId, fileExtension);

            // 4. 上传到 MinIO（覆盖式上传）
            String fileUrl = minioUtil.upload(file.getInputStream(), fileName);

            log.info("用户头像上传成功: userId={}, url={}", userId, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            throw new BusinessException("头像上传失败,请重试");
        }
    }

    @Override
    public String uploadArticleCover(MultipartFile file, Long userId, Long articleId) {
        // 1. 校验文件
        validateFile(file, COVER_MAX_SIZE);

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName;

            if (articleId != null) {
                // 2a. 如果有articleId,生成固定文件名：covers/article_{articleId}_cover.{ext}
                fileName = String.format("covers/article_%d_cover%s", articleId, fileExtension);

                // 3a. 删除旧封面（如果存在不同格式的旧文件）
                deleteOldArticleCovers(articleId, fileExtension);
            } else {
                // 2b. 如果没有articleId(新建文章时),生成临时文件名：covers/user_{userId}_temp_{timestamp}.{ext}
                fileName = String.format("covers/user_%d_temp_%d%s",
                    userId, System.currentTimeMillis(), fileExtension);
            }

            // 4. 上传到 MinIO（覆盖式上传）
            String fileUrl = minioUtil.upload(file.getInputStream(), fileName);

            log.info("文章封面上传成功: userId={}, articleId={}, url={}", userId, articleId, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("文章封面上传失败: userId={}, articleId={}", userId, articleId, e);
            throw new BusinessException("文章封面上传失败,请重试");
        }
    }

    @Override
    public void deleteFile(String fileName) {
        minioUtil.deleteFile(fileName);
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file, long maxSize) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 2. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("只支持上传 JPG、PNG 格式的图片");
        }

        // 3. 校验文件大小
        if (file.getSize() > maxSize) {
            long maxSizeMB = maxSize / 1024 / 1024;
            throw new BusinessException(String.format("图片大小不能超过 %dMB", maxSizeMB));
        }

        // 4. 校验文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw new BusinessException("只支持上传 JPG、PNG 格式的图片");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".jpg")
            || lowerCaseFilename.endsWith(".jpeg")
            || lowerCaseFilename.endsWith(".png");
    }

    /**
     * 删除用户的旧头像文件（处理不同格式的情况）
     */
    private void deleteOldAvatars(Long userId, String currentExtension) {
        // 常见图片格式
        String[] extensions = {".jpg", ".jpeg", ".png"};

        for (String ext : extensions) {
            // 跳过当前要上传的格式（MinIO会自动覆盖）
            if (ext.equalsIgnoreCase(currentExtension)) {
                continue;
            }

            // 删除其他格式的旧头像
            String oldFileName = String.format("avatars/user_%d_avatar%s", userId, ext);
            minioUtil.deleteFile(oldFileName);
        }
    }

    /**
     * 删除文章的旧封面文件（处理不同格式的情况）
     */
    private void deleteOldArticleCovers(Long articleId, String currentExtension) {
        // 常见图片格式
        String[] extensions = {".jpg", ".jpeg", ".png"};

        for (String ext : extensions) {
            // 跳过当前要上传的格式（MinIO会自动覆盖）
            if (ext.equalsIgnoreCase(currentExtension)) {
                continue;
            }

            // 删除其他格式的旧封面
            String oldFileName = String.format("covers/article_%d_cover%s", articleId, ext);
            minioUtil.deleteFile(oldFileName);
        }
    }
}
