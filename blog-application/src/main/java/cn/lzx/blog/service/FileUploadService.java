package cn.lzx.blog.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传Service接口
 */
public interface FileUploadService {

    /**
     * 上传用户头像
     * @param file 头像文件
     * @param userId 用户ID
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 上传文章封面
     * @param file 封面文件
     * @param userId 用户ID
     * @param articleId 文章ID(可选,如果为null则使用临时命名)
     * @return 封面URL
     */
    String uploadArticleCover(MultipartFile file, Long userId, Long articleId);

    /**
     * 删除文件
     * @param fileName 文件名
     */
    void deleteFile(String fileName);
}
