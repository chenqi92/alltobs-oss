# OssTemplate 使用说明

`OssTemplate` 是一个用于操作 AWS S3 或兼容 S3 的对象存储服务的模板类，封装了常用的存储操作，包括创建桶、上传文件、生成预签名 URL、设置对象过期时间等功能。本文档将详细介绍如何使用 `OssTemplate` 类。


## 1. 初始化

在使用 `OssTemplate` 前，需确保已经在 Spring Boot 项目中配置了必要的属性。`OssTemplate` 会在初始化时自动配置 Amazon S3 客户端。

### 1.1 配置示例

```yaml
oss:
  endpoint: https://oss.example.com
  region: us-west-1
  accessKey: YOUR_ACCESS_KEY
  secretKey: YOUR_SECRET_KEY
  bucketName: default-bucket
  expiringBuckets:
    temp-bucket-1: 30  # 30 天后自动删除
    temp-bucket-2: 60  # 60 天后自动删除
```

## 2. 方法说明

### 2.1 创建 Bucket

```java
public void createBucket(String bucketName)
```

**说明**: 创建一个新的存储桶。如果存储桶已经存在，不会重复创建。

**参数**:
- `bucketName`: 存储桶名称

**示例**:
```java
ossTemplate.createBucket("new-bucket");
```

### 2.2 获取全部 Bucket

```java
public List<Bucket> getAllBuckets()
```

**说明**: 获取当前账号下所有的存储桶。

**返回**: `List<Bucket>` 所有存储桶的列表。

**示例**:
```java
List<Bucket> buckets = ossTemplate.getAllBuckets();
```

### 2.3 获取指定 Bucket

```java
public Optional<Bucket> getBucket(String bucketName)
```

**说明**: 获取指定名称的存储桶。

**参数**:
- `bucketName`: 存储桶名称

**返回**: `Optional<Bucket>` 指定的存储桶。

**示例**:
```java
Optional<Bucket> bucket = ossTemplate.getBucket("existing-bucket");
```

### 2.4 删除 Bucket

```java
public void removeBucket(String bucketName)
```

**说明**: 删除指定的存储桶。

**参数**:
- `bucketName`: 存储桶名称

**示例**:
```java
ossTemplate.removeBucket("old-bucket");
```

### 2.5 获取文件前缀查询

```java
public List<S3ObjectSummary> getAllObjectsByPrefix(String bucketName, String prefix)
```

**说明**: 根据文件前缀获取存储桶中的文件。

**参数**:
- `bucketName`: 存储桶名称
- `prefix`: 文件前缀

**返回**: `List<S3ObjectSummary>` 符合前缀的文件列表。

**示例**:
```java
List<S3ObjectSummary> files = ossTemplate.getAllObjectsByPrefix("bucket-name", "prefix");
```

### 2.6 获取文件下载链接

```java
public String getObjectURL(String bucketName, String objectName, int minutes)
```

**说明**: 获取指定文件的下载链接，链接有效期由 `minutes` 参数指定。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 文件名称
- `minutes`: 链接过期时间（分钟）

**返回**: `String` 文件的下载 URL。

**示例**:
```java
String url = ossTemplate.getObjectURL("bucket-name", "file.txt", 60);
```

### 2.7 上传文件

```java
public void putObject(String bucketName, String objectName, InputStream stream) throws IOException
```

**说明**: 上传一个文件到指定的存储桶中。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 文件名称
- `stream`: 文件流

**示例**:
```java
InputStream fileStream = new FileInputStream("path/to/file.txt");
ossTemplate.putObject("bucket-name", "file.txt", fileStream);
```

### 2.8 上传文件并设置过期时间

```java
public void putObject(String bucketName, String objectName, InputStream stream, Date expiresAt) throws IOException
```

**说明**: 上传文件并设置文件的过期时间。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 文件名称
- `stream`: 文件流
- `expiresAt`: 文件的过期时间

**示例**:
```java
Date expiresAt = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
ossTemplate.putObject("bucket-name", "file.txt", fileStream, expiresAt);
```

### 2.9 设置对象的访问权限

```java
public void setObjectAcl(String bucketName, String objectName, CannedAccessControlList acl)
```

**说明**: 设置对象的访问权限。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 对象名称
- `acl`: 访问控制列表（如 `CannedAccessControlList.PublicRead`）

**示例**:
```java
ossTemplate.setObjectAcl("bucket-name", "file.txt", CannedAccessControlList.PublicRead);
```

### 2.10 获取对象标签

```java
public Map<String, String> getObjectTags(String bucketName, String objectName)
```

**说明**: 获取对象的标签。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 对象名称

**返回**: `Map<String, String>` 对象的标签集合。

**示例**:
```java
Map<String, String> tags = ossTemplate.getObjectTags("bucket-name", "file.txt");
```

### 2.11 删除对象

```java
public void removeObject(String bucketName, String objectName)
```

**说明**: 删除指定的对象。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 对象名称

**示例**:
```java
ossTemplate.removeObject("bucket-name", "file.txt");
```

## 3. 高级功能

### 3.1 设置对象加密上传

```java
public PutObjectResult putObjectWithEncryption(String bucketName, String objectName, InputStream stream, long size, String contentType, String sseAlgorithm)
```

**说明**: 上传文件并进行服务器端加密。

**参数**:
- `bucketName`: 存储桶名称
- `objectName`: 文件名称
- `stream`: 文件流
- `size`: 文件大小
- `contentType`: 文件类型
- `sseAlgorithm`: 加密算法（如 `"AES256"`）

**示例**:
```java
ossTemplate.putObjectWithEncryption("bucket-name", "file.txt", fileStream, fileSize, "application/octet-stream", "AES256");
```

### 3.2 设置对象版本控制

```java
public void setBucketVersioning(String bucketName, boolean enable)
```

**说明**: 启用或禁用对象版本控制。

**参数**:
- `bucketName`: 存储桶名称
- `enable`: 是否启用版本控制（`true` 启用，`false` 禁用）

**示例**:
```java
ossTemplate.setBucketVersioning("bucket-name", true);
```

### 3.3 创建子 Bucket 并设置生命周期规则

```java
private void createBucketWithExpiration(String bucketName, int expirationDays)
```

**说明**: 创建子 Bucket，并为其设置自动删除规则。

**参数**:
- `bucketName`: 子 Bucket 名称
- `expirationDays`: 存储天数，超过该天数后自动删除

**示例**:
```java
ossTemplate.createBucketWithExpiration("temp-bucket", 30);
```
