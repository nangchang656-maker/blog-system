package cn.lzx.blog.integration.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.lzx.blog.config.minio.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * minio工具类
 */
@Slf4j
@Component
public class MinioUtil {

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

    static {
        CONTENT_TYPE_MAP.put(".bmp", "image/bmp");
        CONTENT_TYPE_MAP.put(".gif", "image/gif");
        CONTENT_TYPE_MAP.put(".jpeg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".jpg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".png", "image/png");
        CONTENT_TYPE_MAP.put(".html", "text/html");
        CONTENT_TYPE_MAP.put(".txt", "text/plain");
        CONTENT_TYPE_MAP.put(".vsd", "application/vnd.visio");
        CONTENT_TYPE_MAP.put(".pptx", "application/vnd.ms-powerpoint");
        CONTENT_TYPE_MAP.put(".ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPE_MAP.put(".docx", "application/msword");
        CONTENT_TYPE_MAP.put(".doc", "application/msword");
        CONTENT_TYPE_MAP.put(".xml", "text/xml");
    }

    @Resource
    private MinioProperties minioProperties;

    @Autowired
    private MinioClient minioClient;

    /**
     * 判断bucket是否存在，不存在则创建
     */
    public void existBucket(String name) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
                log.info("MinIO bucket created: {}", name);
            }
        } catch (Exception e) {
            log.error("Failed to check or create bucket: {}", name, e);
            throw new RuntimeException("MinIO bucket操作失败: " + name, e);
        }
    }

    /**
     * 文件上传
     *
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @return 文件访问URL
     * @desc 不使用static修饰-注入minioClient
     */
    public String upload(InputStream inputStream, String fileName) {
        if (inputStream == null || fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("输入流和文件名不能为空");
        }

        try {
            // 上传到minio
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType(getContentType(fileName.substring(fileName.lastIndexOf("."))))
                    .build());

            log.info("File uploaded successfully: {}", fileName);
            // 返回上传的文件URL
            return String.format("%s/%s/%s", minioProperties.getEndpoint(),
                    minioProperties.getBucketName(), fileName);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw new RuntimeException("文件上传失败: " + fileName, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("Failed to close input stream", e);
            }
        }
    }

    /**
     * 判断OSS服务文件上传时文件的contentType
     *
     * @param filenameExtension 文件后缀
     * @return String
     */
    private static String getContentType(String filenameExtension) {
        return CONTENT_TYPE_MAP.getOrDefault(filenameExtension.toLowerCase(), "application/octet-stream");
    }

}
