package cn.allbs.oss.service;

import cn.allbs.oss.properties.OssProperties;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类 OssTemplate
 * </p>
 *
 * @author ChenQi
 * &#064;date  2022/11/4 13:45
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class OssTemplate implements InitializingBean {

    private final OssProperties ossProperties;

    private AmazonS3 amazonS3;

    private String BASE_BUCKET;

    /**
     * 创建bucket
     *
     * @param bucketName bucket名称
     */
    public void createBucket(String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket((bucketName));
        }
    }

    /**
     * 获取全部bucket
     * <p>
     *
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS API
     * Documentation</a>
     */
    public List<Bucket> getAllBuckets() {
        return amazonS3.listBuckets();
    }

    /**
     * @param bucketName bucket名称
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS API
     * Documentation</a>
     */
    public Optional<Bucket> getBucket(String bucketName) {
        return amazonS3.listBuckets().stream().filter(b -> b.getName().equals(bucketName)).findFirst();
    }

    /**
     * @param bucketName bucket名称
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteBucket">AWS API
     * Documentation</a>
     */
    public void removeBucket(String bucketName) {
        amazonS3.deleteBucket(bucketName);
    }

    /**
     * 根据文件前置查询文件
     *
     * @param bucketName bucket名称
     * @param prefix     前缀
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListObjects">AWS API
     * Documentation</a>
     */
    public List<S3ObjectSummary> getAllObjectsByPrefix(String bucketName, String prefix) {
        ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix);
        return new ArrayList<>(objectListing.getObjectSummaries());
    }

    /**
     * 获取文件外链，只用于下载
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param minutes    过期时间，单位分钟,请注意该值必须小于7天
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
     */
    public String getObjectURL(String bucketName, String objectName, int minutes) {
        return getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes));
    }

    /**
     * 获取文件外链，只用于下载
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires    过期时间,请注意该值必须小于7天
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
     */
    public String getObjectURL(String bucketName, String objectName, Duration expires) {
        return getObjectURL(bucketName, objectName, expires, HttpMethod.GET);
    }

    /**
     * 获取文件上传外链，只用于上传
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param minutes    过期时间，单位分钟,请注意该值必须小于7天
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
     */
    public String getPutObjectURL(String bucketName, String objectName, int minutes) {
        return getPutObjectURL(bucketName, objectName, Duration.ofMinutes(minutes));
    }

    /**
     * 获取文件上传外链，只用于上传
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires    过期时间,请注意该值必须小于7天
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
     */
    public String getPutObjectURL(String bucketName, String objectName, Duration expires) {
        return getObjectURL(bucketName, objectName, expires, HttpMethod.PUT);
    }

    /**
     * 获取文件外链
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param minutes    过期时间，单位分钟,请注意该值必须小于7天
     * @param method     文件操作方法：GET（下载）、PUT（上传）
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration,
     * HttpMethod method)
     */
    public String getObjectURL(String bucketName, String objectName, int minutes, HttpMethod method) {
        return getObjectURL(bucketName, objectName, Duration.ofMinutes(minutes), method);
    }

    /**
     * 获取文件外链
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires    过期时间，请注意该值必须小于7天
     * @param method     文件操作方法：GET（下载）、PUT（上传）
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration,
     * HttpMethod method)
     */
    public String getObjectURL(String bucketName, String objectName, Duration expires, HttpMethod method) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        // Set the pre-signed URL to expire after `expires`.
        Date expiration = Date.from(Instant.now().plus(expires));

        // Generate the pre-signed URL.
        URL url = amazonS3.generatePresignedUrl(new GeneratePresignedUrlRequest(bucketName, objectName).withMethod(method).withExpiration(expiration));
        return url.toString();
    }

    /**
     * 获取文件URL
     * <p>
     * If the object identified by the given bucket and key has public read permissions
     * (ex: {@link CannedAccessControlList#PublicRead}), then this URL can be directly
     * accessed to retrieve the object's data.
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return url
     */
    public String getObjectURL(String bucketName, String objectName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        URL url = amazonS3.getUrl(bucketName, objectName);
        return url.toString();
    }

    /**
     * 获取文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 二进制流
     * @see <a href= "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS
     * API Documentation</a>
     */
    public S3Object getObject(String bucketName, String objectName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        return amazonS3.getObject(bucketName, objectName);
    }

    /**
     * 获取文件流
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 文件流
     */
//    public InputStreamResource getInputStream(String bucketName, String objectName) {
//        // 检索MinIO对象
//        InputStream object = amazonS3.getObject(bucketName, objectName).getObjectContent();
//
//        // 设置响应头
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//        headers.setContentDispositionFormData("attachment", objectName);
//
//        // 创建文件流资源并返回响应
//        InputStreamResource resource = new InputStreamResource(object);
//        return resource;
//    }

    /**
     * 上传文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件流
     * @throws IOException IOException
     */
    public void putObject(String bucketName, String objectName, InputStream stream) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /**
     * 上传文件 指定 contextType
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件流
     * @param contextType 文件类型
     * @throws IOException IOException
     */
    public void putObject(String bucketName, String objectName, String contextType, InputStream stream) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), contextType);
    }

    /**
     * 上传文件
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件流
     * @param size        大小
     * @param contextType 类型
     * @see <a href= "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/PutObject">AWS
     * API Documentation</a>
     */
    public PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size, String contextType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contextType);
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, stream, objectMetadata);
        // Setting the read limit value to one byte greater than the size of stream will
        // reliably avoid a ResetException
        putObjectRequest.getRequestClientOptions().setReadLimit(Long.valueOf(size).intValue() + 1);
        return amazonS3.putObject(putObjectRequest);

    }

    /**
     * 上传文件并设置过期时间
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件流
     * @param expiresAt  过期时间
     * @throws IOException IOException
     */
    public void putObject(String bucketName, String objectName, InputStream stream, Date expiresAt) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream", expiresAt);
    }

    /**
     * 上传文件并设置过期时间，指定 contentType
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件流
     * @param contentType 文件类型
     * @param expiresAt   过期时间
     * @throws IOException IOException
     */
    public void putObject(String bucketName, String objectName, String contentType, InputStream stream, Date expiresAt) throws IOException {
        putObject(bucketName, objectName, stream, stream.available(), contentType, expiresAt);
    }

    /**
     * 上传文件并设置过期时间
     *
     * @param bucketName  bucket名称
     * @param objectName  文件名称
     * @param stream      文件流
     * @param size        文件大小
     * @param contentType 文件类型
     * @param expiresAt   过期时间
     */
    public PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size, String contentType, Date expiresAt) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contentType);
        objectMetadata.setHttpExpiresDate(expiresAt);  // 设置文件过期时间
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, stream, objectMetadata);
        // 设置读取限制值，避免 ResetException
        putObjectRequest.getRequestClientOptions().setReadLimit(Long.valueOf(size).intValue() + 1);
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * 上传文件并设置自动删除时间
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream     文件流
     * @param duration   文件的存活时长，单位为时间单位
     * @param timeUnit   时间单位，如 TimeUnit.DAYS 表示天
     * @throws IOException IOException
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
     * @param stream      文件流
     * @param contentType 文件类型
     * @param duration    文件的存活时长，单位为时间单位
     * @param timeUnit    时间单位，如 TimeUnit.DAYS 表示天
     * @throws IOException IOException
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
     * @param stream      文件流
     * @param size        文件大小
     * @param contentType 文件类型
     * @param duration    文件的存活时长，单位为时间单位
     * @param timeUnit    时间单位，如 TimeUnit.DAYS 表示天
     */
    public PutObjectResult putObjectWithExpiration(String bucketName, String objectName, InputStream stream, long size, String contentType, long duration, TimeUnit timeUnit) {
        Date expirationDate = new Date(System.currentTimeMillis() + timeUnit.toMillis(duration));
        return putObject(bucketName, objectName, stream, size, contentType, expirationDate);
    }

    /**
     * 获取文件信息
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @see <a href= "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS
     * API Documentation</a>
     */
    public S3Object getObjectInfo(String bucketName, String objectName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        return amazonS3.getObject(bucketName, objectName);
    }

    /**
     * 删除文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteObject">AWS API
     * Documentation</a>
     */
    public void removeObject(String bucketName, String objectName) {
        if (StringUtils.hasText(BASE_BUCKET)) {
            objectName = getBucketName(bucketName, objectName);
            bucketName = BASE_BUCKET;
        }
        amazonS3.deleteObject(bucketName, objectName);
    }

    /**
     * 初始化多部分上传
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return 上传ID
     */
    public String initiateMultipartUpload(String bucketName, String objectName) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
        InitiateMultipartUploadResult result = amazonS3.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    /**
     * 上传文件并进行服务器端加密
     *
     * @param bucketName   bucket名称
     * @param objectName   文件名称
     * @param stream       文件流
     * @param size         大小
     * @param contentType  类型
     * @param sseAlgorithm 加密算法 (e.g., "AES256")
     */
    public PutObjectResult putObjectWithEncryption(String bucketName, String objectName, InputStream stream, long size, String contentType, String sseAlgorithm) {
        // 创建对象元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        metadata.setSSEAlgorithm(sseAlgorithm);  // 设置服务器端加密算法

        // 创建PutObjectRequest
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, stream, metadata);

        // 上传对象并返回结果
        return amazonS3.putObject(putObjectRequest);
    }

    /**
     * 完成多部分上传
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param uploadId   上传ID
     * @param partETags  上传块的ETag列表
     * @return 上传结果
     */
    public CompleteMultipartUploadResult completeMultipartUpload(String bucketName, String objectName, String uploadId, List<PartETag> partETags) {
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags);
        return amazonS3.completeMultipartUpload(request);
    }


    /**
     * 启用或禁用对象版本控制
     *
     * @param bucketName bucket名称
     * @param enable     是否启用版本控制
     */
    public void setBucketVersioning(String bucketName, boolean enable) {
        SetBucketVersioningConfigurationRequest versioningConfigurationRequest =
                new SetBucketVersioningConfigurationRequest(
                        bucketName,
                        new BucketVersioningConfiguration()
                                .withStatus(enable ? BucketVersioningConfiguration.ENABLED : BucketVersioningConfiguration.SUSPENDED)
                );
        amazonS3.setBucketVersioningConfiguration(versioningConfigurationRequest);
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
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey);
        amazonS3.copyObject(copyObjectRequest);
    }

    /**
     * 设置对象的访问权限
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param acl        访问控制列表（例如CannedAccessControlList.PublicRead）
     */
    public void setObjectAcl(String bucketName, String objectName, CannedAccessControlList acl) {
        amazonS3.setObjectAcl(bucketName, objectName, acl);
    }

    /**
     * 获取对象的访问权限
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return ACL信息
     */
    public AccessControlList getObjectAcl(String bucketName, String objectName) {
        return amazonS3.getObjectAcl(bucketName, objectName);
    }

    /**
     * 设置对象标签
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param tags       标签集合
     */
    public void setObjectTags(String bucketName, String objectName, Map<String, String> tags) {
        ObjectTagging tagging = new ObjectTagging(tags.entrySet().stream()
                .map(entry -> new Tag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        amazonS3.setObjectTagging(new SetObjectTaggingRequest(bucketName, objectName, tagging));
    }

    /**
     * 获取对象标签
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return 标签集合
     */
    public Map<String, String> getObjectTags(String bucketName, String objectName) {
        GetObjectTaggingResult taggingResult = amazonS3.getObjectTagging(new GetObjectTaggingRequest(bucketName, objectName));
        return taggingResult.getTagSet().stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化AmazonS3客户端
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(ossProperties.getEndpoint(), ossProperties.getRegion());
        AWSCredentials awsCredentials = new BasicAWSCredentials(ossProperties.getAccessKey(), ossProperties.getSecretKey());
        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        this.amazonS3 = AmazonS3Client.builder().withEndpointConfiguration(endpointConfiguration).withClientConfiguration(clientConfiguration).withCredentials(awsCredentialsProvider).disableChunkedEncoding().withPathStyleAccessEnabled(ossProperties.getPathStyleAccess()).build();
        BASE_BUCKET = ossProperties.getBucketName();

        // 创建默认的存储桶
        if (StringUtils.hasText(BASE_BUCKET)) {
            createBucket(BASE_BUCKET);
        }

        // 为每个子 Bucket 设置生命周期规则
        if (ossProperties.getExpiringBuckets() != null) {
            ossProperties.getExpiringBuckets().forEach(this::createBucketWithExpiration);
        }
    }

    /**
     * 创建子 Bucket 并设置生命周期规则
     *
     * @param bucketName     子 Bucket 名称
     * @param expirationDays 存储天数，超过该天数后自动删除
     */
    private void createBucketWithExpiration(String bucketName, int expirationDays) {
        createBucket(bucketName);

        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
                .withId("AutoDeleteRule")
                .withExpirationInDays(expirationDays)
                .withStatus(BucketLifecycleConfiguration.ENABLED);

        BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration()
                .withRules(Collections.singletonList(rule));

        amazonS3.setBucketLifecycleConfiguration(bucketName, configuration);
    }

    /**
     * 获取bucketName
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return bucketName
     */
    private String getBucketName(String bucketName, String objectName) {
        if (StringUtils.hasText(bucketName)) {
            return bucketName + "/" + objectName;
        }
        return objectName;
    }

}
