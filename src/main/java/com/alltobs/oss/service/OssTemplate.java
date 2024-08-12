package com.alltobs.oss.service;

import com.alltobs.oss.properties.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OssTemplate类封装了对S3服务的操作方法，包括文件上传、下载、删除、目录创建等。
 * 当BASE_BUCKET不为空时，所有操作都基于BASE_BUCKET进行；否则基于传入的bucketName进行。
 * </p>
 *
 * @author ChenQi
 * &#064;date  2024/08/09 13:45
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class OssTemplate implements InitializingBean {

    private final OssProperties ossProperties;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    private String BASE_BUCKET;

    @Override
    public void afterPropertiesSet() throws Exception {
        s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        ossProperties.getAccessKey(), ossProperties.getSecretKey())))
                .region(Region.of(ossProperties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(ossProperties.getPathStyleAccess())
                        .build())
                .endpointOverride(URI.create(ossProperties.getEndpoint()))
                .build();

        s3Presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        ossProperties.getAccessKey(), ossProperties.getSecretKey())))
                .region(Region.of(ossProperties.getRegion()))
                .endpointOverride(URI.create(ossProperties.getEndpoint()))
                .build();

        BASE_BUCKET = ossProperties.getBucketName();

        // 创建默认的存储桶
        if (StringUtils.hasText(BASE_BUCKET) && !isBucketExist(BASE_BUCKET)) {
            createBucket(BASE_BUCKET);
        }

        // 为每个子目录设置生命周期规则
        if (ossProperties.getExpiringBuckets() != null) {
            ossProperties.getExpiringBuckets().forEach(this::createBucketFolderWithExpiration);
        }
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName 桶名称
     * @return 是否存在
     */
    public boolean isBucketExist(String bucketName) {
        return s3Client.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName));
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName 桶名称
     * @return 是否存在
     */
    public boolean doesBucketOrFolderExist(String bucketName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            // 构建目标前缀，确保 folderName 以 "/" 结尾
            String targetPrefix = bucketName.endsWith("/") ? bucketName : bucketName + "/";
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BASE_BUCKET)
                    .prefix(targetPrefix)
                    .delimiter("/")
                    .build());
            return !response.contents().isEmpty();
        } else {
            return s3Client.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName));
        }
    }

    /**
     * 检查桶是否包含指定目录
     *
     * @param bucketName 桶名称
     * @param folderName 目录名称
     * @return 是否存在
     */
    public boolean doesBucketContainFolder(String bucketName, String folderName) {

        // 构建目标前缀，确保 folderName 以 "/" 结尾
        String targetPrefix = folderName.endsWith("/") ? folderName : folderName + "/";

        // 列出 bucket 下的所有对象，以目标前缀为筛选条件
        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(targetPrefix)
                .delimiter("/")
                .build());

        // 如果存在bucket，返回 true，否则返回 false
        return !response.contents().isEmpty();
    }

    /**
     * 创建桶或目录
     * 如果 BASE_BUCKET 不为空，则在 BASE_BUCKET 下创建目录。
     * 否则，直接创建桶。
     *
     * @param bucketName 目标bucket名称或目录名称
     */
    public void createBucket(String bucketName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            // 如果 BASE_BUCKET 不为空，则在 BASE_BUCKET 下创建子目录
            putObject(BASE_BUCKET, bucketName + "/", new byte[0]);
        } else {
            // 检查是否已存在同名桶（或目录），如果不存在则创建
            if (!isBucketExist(bucketName)) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            }
        }
    }

    /**
     * 获取所有桶
     *
     * @return 所有bucket的列表
     */
    public List<String> getAllBuckets() {
        // 如果 BASE_BUCKET 存在，则获取 BASE_BUCKET 目录下的所有“桶”
        if (StringUtils.hasText(BASE_BUCKET)) {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BASE_BUCKET)
                    .delimiter("/")
                    .build());

            // 获取所有以 '/' 结尾的“文件夹”名称
            return response.commonPrefixes().stream()
                    .map(prefix -> prefix.prefix().replaceAll("/$", "")) // 去除末尾的 '/'
                    .collect(Collectors.toList());
        } else {
            // 否则返回所有顶级桶
            return s3Client.listBuckets().buckets().stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取 BASE_BUCKET 中指定名称的桶（即目录）的属性
     *
     * @param bucketName 目录名称
     * @return 包含桶属性的 Map 对象，包含大小、过期时间等信息
     */
    public Map<String, Object> getBucketProperties(String bucketName) {
        Map<String, Object> properties = new HashMap<>();
        String targetPrefix = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" : "";
        String finalBucketName = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;

        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(finalBucketName)
                .prefix(targetPrefix)
                .build());

        long totalSize = response.contents().stream().mapToLong(S3Object::size).sum();
        properties.put("size", totalSize);

        try {
            GetBucketLifecycleConfigurationResponse lifecycleConfig = s3Client.getBucketLifecycleConfiguration(
                    GetBucketLifecycleConfigurationRequest.builder().bucket(finalBucketName).build());

            Optional<LifecycleRule> relatedRule = lifecycleConfig.rules().stream()
                    .filter(rule -> rule.filter().prefix().equals(targetPrefix))
                    .findFirst();

            properties.put("lifecycleRules", relatedRule.orElse(null));
        } catch (Exception e) {
            properties.put("lifecycleRules", "No lifecycle configuration");
        }

        return properties;
    }

    /**
     * 删除指定桶或 BASE_BUCKET 下的目录
     *
     * @param bucketName bucket名称
     */
    public void removeBucket(String bucketName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            // 如果 BASE_BUCKET 不为空，删除的是 BASE_BUCKET 下的目录
            String objectName = bucketName + "/";
            removeObject(BASE_BUCKET, objectName);
        } else {
            s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        }
    }

    /**
     * 获取指定前缀的所有对象
     *
     * @param bucketName bucket名称
     * @param prefix     对象前缀
     * @return 对象列表
     */
    public List<S3Object> getAllObjectsByPrefix(String bucketName, String prefix) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            // 构建完整的前缀路径
            if (StringUtils.hasText(bucketName)) {
                prefix = bucketName + "/" + (prefix.startsWith("/") ? prefix.substring(1) : prefix);
            }

            // 确保前缀与 BASE_BUCKET 结合
            bucketName = BASE_BUCKET;
        }

        // 创建 ListObjectsV2Request，带上前缀
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)  // 以指定的前缀开头
                .build();

        // 获取符合条件的所有对象
        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents();
    }


    /**
     * 获取文件的下载URL，并设置有效期
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param minutes    URL有效期，单位为分钟
     * @return 生成的URL
     */
    public String getObjectURL(String bucketName, String objectName, int minutes) {
        return getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes));
    }

    /**
     * 获取文件的下载URL，并设置有效期
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires    URL有效期
     * @return 生成的URL
     */
    public String getObjectURL(String bucketName, String objectName, Duration expires) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        PresignedGetObjectRequest getObjectRequest = s3Presigner.presignGetObject(builder -> builder
                .getObjectRequest(b -> b.bucket(targetBucket).key(targetObjectName))
                .signatureDuration(expires));

        return getObjectRequest.url().toString();
    }

    /**
     * 获取文件的URL
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 文件URL
     */
    public String getObjectURL(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        GetUrlRequest getUrlRequest = GetUrlRequest.builder().bucket(targetBucket).key(targetObjectName).build();
        URL url = s3Client.utilities().getUrl(getUrlRequest);
        return url.toString();
    }

    /**
     * 下载文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 文件的二进制流
     */
    public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        return s3Client.getObject(GetObjectRequest.builder().bucket(targetBucket).key(targetObjectName).build());
    }

    /**
     * 上传文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件输入流
     * @throws IOException IO异常
     */
    public void putObject(String bucketName, String objectName, InputStream stream) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /**
     * 上传文件，指定contentType
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param contextType 文件类型
     * @throws IOException IO异常
     */
    public void putObject(String bucketName, String objectName, String contextType, InputStream stream) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), contextType);
    }

    /**
     * 上传文件
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param size        文件大小
     * @param contextType 文件类型
     * @return 上传响应对象
     */
    public PutObjectResponse putObject(String bucketName, String objectName, InputStream stream, long size, String contextType) throws IOException {
        if (!doesBucketOrFolderExist(bucketName)) {
            createBucket(bucketName);
        }

        String finalBucketName = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String finalObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(finalBucketName)
                .key(finalObjectName)
                .contentLength(size)
                .contentType(contextType)
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
    }

    /**
     * 上传文件并设置过期时间
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件输入流
     * @param expiresAt  过期时间
     * @throws IOException IO异常
     */
    public void putObject(String bucketName, String objectName, InputStream stream, Date expiresAt) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream", expiresAt);
    }

    /**
     * 上传文件并设置过期时间，指定 contentType
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param contentType 文件类型
     * @param expiresAt   过期时间
     * @throws IOException IO异常
     */
    public void putObject(String bucketName, String objectName, String contentType, InputStream stream, Date expiresAt) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), contentType, expiresAt);
    }

    /**
     * 上传文件并设置过期时间
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @param expiresAt   过期时间
     * @return 上传响应对象
     */
    public PutObjectResponse putObject(String bucketName, String objectName, InputStream stream, long size, String contentType, Date expiresAt) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .contentLength(size)
                .contentType(contentType)
                .expires(expiresAt.toInstant())
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
    }

    /**
     * 上传空文件以创建目录
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param content    文件内容
     */
    public void putObject(String bucketName, String objectName, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .contentLength((long) content.length)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    /**
     * 上传文件并设置自动删除时间
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件输入流
     * @param duration   文件的存活时长，单位为时间单位
     * @param timeUnit   时间单位，如 TimeUnit.DAYS 表示天
     * @throws IOException IO异常
     */
    public void putObjectWithExpiration(String bucketName, String objectName, InputStream stream, long duration, TimeUnit timeUnit) throws IOException {
        Date expirationDate = new Date(System.currentTimeMillis() + timeUnit.toMillis(duration));
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream", expirationDate);
    }

    /**
     * 上传文件并设置自动删除时间，指定 contentType
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param contentType 文件类型
     * @param duration    文件的存活时长，单位为时间单位
     * @param timeUnit    时间单位，如 TimeUnit.DAYS 表示天
     * @throws IOException IO异常
     */
    public void putObjectWithExpiration(String bucketName, String objectName, String contentType, InputStream stream, long duration, TimeUnit timeUnit) throws IOException {
        Date expirationDate = new Date(System.currentTimeMillis() + timeUnit.toMillis(duration));
        putObject(bucketName, objectName, stream, stream.available(), contentType, expirationDate);
    }

    /**
     * 上传文件并设置自动删除时间
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     * @param duration    文件的存活时长，单位为时间单位
     * @param timeUnit    时间单位，如 TimeUnit.DAYS 表示天
     * @return 上传响应对象
     */
    public PutObjectResponse putObjectWithExpiration(String bucketName, String objectName, InputStream stream, long size, String contentType, long duration, TimeUnit timeUnit) {
        Date expirationDate = new Date(System.currentTimeMillis() + timeUnit.toMillis(duration));
        return putObject(bucketName, objectName, stream, size, contentType, expirationDate);
    }

    /**
     * 获取文件信息
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 文件信息
     */
    public HeadObjectResponse getObjectInfo(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        return s3Client.headObject(HeadObjectRequest.builder().bucket(targetBucket).key(targetObjectName).build());
    }

    /**
     * 删除文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     */
    public void removeObject(String bucketName, String objectName) {
        String finalBucketName = StringUtils.hasText(BASE_BUCKET) && !bucketName.equals(BASE_BUCKET)
                ? BASE_BUCKET
                : bucketName;
        String finalObjectName = StringUtils.hasText(BASE_BUCKET) && !bucketName.equals(BASE_BUCKET)
                ? bucketName + "/" + objectName
                : objectName;

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(finalBucketName)
                .key(finalObjectName)
                .build());
    }


    /**
     * 初始化多部分上传
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return 上传ID
     */
    public String initiateMultipartUpload(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .build());
        return response.uploadId();
    }

    /**
     * 上传文件并进行服务器端加密
     *
     * @param bucketName   bucket名称
     * @param objectName   文件名称
     * @param stream       文件输入流
     * @param size         文件大小
     * @param contentType  文件类型
     * @param sseAlgorithm 加密算法 (e.g., "AES256")
     * @return 上传响应对象
     */
    public PutObjectResponse putObjectWithEncryption(String bucketName, String objectName, InputStream stream, long size, String contentType, String sseAlgorithm) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .contentLength(size)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.fromValue(sseAlgorithm))
                .build();

        return s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
    }

    /**
     * 完成多部分上传
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param uploadId   上传ID
     * @param partETags  上传块的ETag列表
     * @return 上传响应对象
     */
    public CompleteMultipartUploadResponse completeMultipartUpload(String bucketName, String objectName, String uploadId, List<CompletedPart> partETags) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(partETags).build();

        return s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build());
    }

    /**
     * 启用或禁用对象版本控制
     *
     * @param bucketName bucket名称
     * @param enable     是否启用版本控制
     */
    public void setBucketVersioning(String bucketName, boolean enable) {
        String status = enable ? "Enabled" : "Suspended";

        s3Client.putBucketVersioning(PutBucketVersioningRequest.builder()
                .bucket(bucketName)
                .versioningConfiguration(VersioningConfiguration.builder()
                        .status(status)
                        .build())
                .build());
    }

    /**
     * 复制对象
     *
     * @param sourceBucketName      源bucket名称
     * @param sourceKey             源对象key
     * @param destinationBucketName 目标bucket名称
     * @param destinationKey        目标对象key
     */
    public void copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) {
        String targetSourceKey = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET + "/" + sourceKey : sourceKey;
        String targetDestinationKey = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET + "/" + destinationKey : destinationKey;

        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(sourceBucketName)
                .sourceKey(targetSourceKey)
                .destinationBucket(destinationBucketName)
                .destinationKey(targetDestinationKey)
                .build();
        s3Client.copyObject(copyObjectRequest);
    }

    /**
     * 设置对象的访问权限
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param acl        访问控制列表（例如CannedAccessControlList.PublicRead）
     */
    public void setObjectAcl(String bucketName, String objectName, String acl) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        s3Client.putObjectAcl(PutObjectAclRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .acl(acl)
                .build());
    }

    /**
     * 获取对象的访问权限
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return ACL信息
     */
    public GetObjectAclResponse getObjectAcl(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        return s3Client.getObjectAcl(GetObjectAclRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .build());
    }

    /**
     * 设置对象标签
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param tags       标签集合
     */
    public void setObjectTags(String bucketName, String objectName, Map<String, String> tags) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        Tagging tagging = Tagging.builder()
                .tagSet(tags.entrySet().stream()
                        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                        .collect(Collectors.toList()))
                .build();

        s3Client.putObjectTagging(PutObjectTaggingRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .tagging(tagging)
                .build());
    }

    /**
     * 获取对象标签
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return 标签集合
     */
    public Map<String, String> getObjectTags(String bucketName, String objectName) {
        String targetBucket = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : bucketName;
        String targetObjectName = StringUtils.hasText(BASE_BUCKET) ? bucketName + "/" + objectName : objectName;

        GetObjectTaggingResponse taggingResponse = s3Client.getObjectTagging(GetObjectTaggingRequest.builder()
                .bucket(targetBucket)
                .key(targetObjectName)
                .build());

        return taggingResponse.tagSet().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }

    /**
     * 创建子目录并设置生命周期规则
     *
     * @param subBucketName  子目录名称
     * @param expirationDays 存储天数，超过该天数后自动删除
     */
    private void createBucketFolderWithExpiration(String subBucketName, int expirationDays) {
        // 基于 BASE_BUCKET 创建目录
        createBucket(subBucketName);

        String folderPrefix = StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET + "/" + subBucketName + "/" : subBucketName + "/";

        // 设置生命周期规则
        LifecycleRule rule = LifecycleRule.builder()
                .id("AutoDeleteRule-" + subBucketName)
                .filter(LifecycleRuleFilter.builder().prefix(folderPrefix).build())
                .expiration(LifecycleExpiration.builder().days(expirationDays).build())
                .status(ExpirationStatus.ENABLED)
                .build();

        PutBucketLifecycleConfigurationRequest configurationRequest = PutBucketLifecycleConfigurationRequest.builder()
                .bucket(StringUtils.hasText(BASE_BUCKET) ? BASE_BUCKET : subBucketName)
                .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(rule).build())
                .build();

        try {
            s3Client.putBucketLifecycleConfiguration(configurationRequest);
        } catch (Exception e) {
            System.err.println("创建子目录并设置生命周期规则失败：" + e.getMessage());
        }
    }
}
