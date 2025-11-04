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
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
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
     * 初始化：确保bucket存在并设置为公开访问
     */
    @PostConstruct
    public void init() {
        try {
            String bucketName = minioProperties.getBucketName();

            // 1. 检查并创建 bucket
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket created: {}", bucketName);
            }

            // 2. 设置 bucket 为公开读取（允许匿名访问）
            String policy = getPolicyJson(bucketName);
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build()
            );
            log.info("MinIO bucket policy set to public read: {}", bucketName);
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            throw new RuntimeException("MinIO 初始化失败", e);
        }
    }

    /**
     * 生成公开读取的 bucket 策略
     */
    private String getPolicyJson(String bucketName) {
        return """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": ["*"]
                  },
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """.formatted(bucketName);
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

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 文件是否存在
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件名为空，跳过删除操作");
            return;
        }

        try {
            // 检查文件是否存在
            if (!fileExists(fileName)) {
                log.info("文件不存在，无需删除: {}", fileName);
                return;
            }

            // 删除文件
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .build());

            log.info("文件删除成功: {}", fileName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileName, e);
            // 删除失败不抛异常，避免影响主流程
        }
    }

}
