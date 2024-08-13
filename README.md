## 依赖jar包
| 引入包                       | 版本       |
|---------------------------|----------|
| jdk                       | 17       |
| spring boot               | 3.3.42   |
| spring-boot-autoconfigure | 3.3.42   |
| jakarta.validation-api    | 3.1.0    |
| s3                        | 2.27.1 |
| sts                       | 2.27.1 |
| auth                      | 2.27.1 |

## 使用
### 添加依赖

```xml
<dependency>
  <groupId>com.alltobs</groupId>
  <artifactId>alltobs-oss</artifactId>
  <version>1.0.0</version>
</dependency>
```

### 操作方法使用说明
[查看使用说明](./METHOD.md)

### demo地址
[demo地址](https://github.com/chenqi92/alltobs-demo/tree/master/alltobs-oss-demo)



## 前置测试环境
首先使用docker-compose安装了最新的minio用于测试
```yml
version: '3'

  minio:
    image: minio/minio
    container_name: minio
    restart: always
    ports:
      - "9000:9000" # api端口
      - "9001:9001" # 控制台端口
    environment:
      MINIO_ROOT_USER: "miniouser"      # 设置你的访问账户(用于控制台访问)
      MINIO_ROOT_PASSWORD: "123456789"  # 设置你的访问密钥(用于控制台访问)
    volumes:
      - /mnt/minio/files:/data                   # 将容器中的 /data 文件夹挂载到主机上的指定文件夹 我使用的是/mnt/minio/files
    command: server /data --console-address ":9001"
```

## 项目引入依赖
这里说明一下cn.allbs这个group用于jdk1.8，com.alltobs用于jdk17+，但是cn.allbs中有一点点jdk17，遇到的话降级点版本。
```xml
<dependency>
  <groupId>com.alltobs</groupId>
  <artifactId>alltobs-oss</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 项目配置
### 配置文件
```yaml
oss:
  endpoint: http://xxx.xxx.xxx.xxx:9000
  # 所在地区
  region: cn-north-1
  # minio账号或者
  access-key: adadmin
  # 密码
  secret-key: 123456778
  # 设置一个默认的文件桶，比如不同项目使用同一个文件库，以项目为文件桶分隔
  bucket-name: test
  # 设置会过期的子文件桶
  expiring-buckets:
    temp-bucket-1: 30  # 生命周期30天
    temp-bucket-2: 60  # 生命周期60天
```

- `endpoint`就是安装minio或者腾讯云、阿里云之类的对象储存地址
- `region`所在地区
- `access-key`可以直接使用控制台账号，但是建议在控制台中生成账号和密钥
- `secret-key`可以直接使用控制台密钥，但是建议在控制台中生成账号和密钥
- `bucket-name` 设置一个默认的文件桶，比如不同项目使用同一个文件库，以项目为文件桶分隔
- `expiring-buckets` 里面设置的是生命周期会过期的文件夹，创建位置位于bucket-name下，比如bucket-name叫test,那么会在test目录下创建一个生命周期为30天的文件夹temp-bucket-1，生命周期为60天的文件夹temp-bucket-2

###  启动类注解`@EnableAllbsOss`
![启用](https://nas.allbs.cn:9006/cloudpic/2024/08/7015d4fb5a252f2db7a2a62e02298d6f.png)
### 使用引入
```
@Resource
private OssTemplate ossTemplate;
```
![使用](https://nas.allbs.cn:9006/cloudpic/2024/08/7f430e2f172c605c669875fd124f56c1.png)


## 使用演示
以下所有方法都遵循一个原则，当bucket-name不为空时，所有操作都在这个bucket下进行。
### 自动创建主目录和带过期时间的子目录
![配置](https://nas.allbs.cn:9006/cloudpic/2024/08/ceed18a32c4f64bd00a0b3ca775a8955.png)
就会自动创建文件桶如下，一个位于`test`目录下且十天后会自动删除的`expire-bucket-1`
![创建目录](https://nas.allbs.cn:9006/cloudpic/2024/08/d35e2d1016aa6bd02c4f5bca55cb0a6c.png)

### 创建bucket
```java
@PostMapping("/createBucket")  
public ResponseEntity<String> createBucket(@RequestParam String bucketName) {  
    ossTemplate.createBucket(bucketName);  
    return ResponseEntity.ok("Bucket created: " + bucketName);  
}
```
![基于base-bucket创建文件桶](https://nas.allbs.cn:9006/cloudpic/2024/08/9581e176a9d3e94f361b6811b6b20312.png)
![bucket中的bucket](https://nas.allbs.cn:9006/cloudpic/2024/08/1370dd2a25a6e947d3ab35688407cbec.png)


### 查询所有bucket
```java
@GetMapping("/getAllBuckets")  
public R<List<String>> getAllBuckets() {  
    return R.ok(ossTemplate.getAllBuckets());  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/1ced808de75b7b68fe6d02e0292372b2.png)

### 删除指定bucket
```
@DeleteMapping("/removeBucket")  
public R<String> removeBucket(@RequestParam String bucketName) {  
    ossTemplate.removeBucket(bucketName);  
    return R.ok("Bucket removed: " + bucketName);  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/c7ed97e07519f9b1fd7fc7435c1b2663.png)

### 上传文件
```java
@PostMapping("/putObject")  
public R<String> putObject(@RequestParam String bucketName, @RequestParam MultipartFile file) {  
    String fileName = file.getOriginalFilename();  
    try {  
        String uuid = UUID.randomUUID() + "." + FileUtil.getFileType(fileName);  
        ossTemplate.putObject(bucketName, uuid, file.getInputStream());  
        return R.ok("File uploaded: " + uuid);  
    } catch (IOException e) {  
        return R.fail("Failed to upload file: " + fileName);  
    }  
}
```

![上传文件](https://nas.allbs.cn:9006/cloudpic/2024/08/8ba27f26a86924f8464994c280dd1416.png)

### 查询指定目录下指定前缀的文件
```java
@GetMapping("/getAllObjectsByPrefix")  
public R<Set<String>> getAllObjectsByPrefix(@RequestParam String bucketName, @RequestParam String prefix) {   
    return R.ok(ossTemplate.getAllObjectsByPrefix(bucketName, prefix).stream().map(S3Object::key).collect(Collectors.toSet()));  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/c47783072c6ae608c185d7c96062e2a0.png)


### 查看文件(字节数组)
```java
@GetMapping("/getObject")  
public R<byte[]> getObject(@RequestParam String bucketName, @RequestParam String objectName) {  
    try (var s3Object = ossTemplate.getObject(bucketName, objectName)) {  
        return R.ok(s3Object.readAllBytes());  
    } catch (IOException e) {  
        return R.fail(e.getLocalizedMessage());  
    }  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/d13f4710950e785aca116d36cc359029.png)

### 下载文件
```java
@GetMapping("/download")  
public void download(@RequestParam String bucketName, @RequestParam String objectName, HttpServletResponse response) {  
    try (ResponseInputStream<GetObjectResponse> inputStream = ossTemplate.getObject(bucketName, objectName);  
         OutputStream outputStream = response.getOutputStream()) {  
  
        // 获取文件的Content-Type  
        String contentType = inputStream.response().contentType();  
        response.setContentType(contentType);  
  
        // 设置响应头：Content-Disposition，用于浏览器下载文件时的文件名  
        response.setHeader("Content-Disposition", "attachment; filename=\"" + objectName + "\"");  
  
        // 直接将输入流中的数据传输到输出流  
        inputStream.transferTo(outputStream);  
  
        // 刷新输出流，确保所有数据已写出  
        outputStream.flush();  
  
    } catch (IOException e) {  
        // 如果出错，设置响应状态为404  
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);  
    }  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/c83fd31b64bcef1e75269a1bf1c77d38.png)


### 查看文件链接带过期时间
```java
@GetMapping("/getObjectURL")  
public R<String> getObjectURL(@RequestParam String bucketName, @RequestParam String objectName, @RequestParam int minutes) {  
    return R.ok(ossTemplate.getObjectURL(bucketName, objectName, minutes));  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/1bb5020b7b99ca00cac7bbeb30e6b938.png)

### 删除文件
```java
@DeleteMapping("/removeObject")  
public R<String> removeObject(@RequestParam String bucketName, @RequestParam String objectName) {  
    ossTemplate.removeObject(bucketName, objectName);  
    return R.ok("Object removed: " + objectName);  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/45b5b7d145519bedd804abc9056a044a.png)

### 文件复制
```java
@PostMapping("/copyObject")  
public R<String> copyObject(@RequestParam String sourceBucketName, @RequestParam String sourceKey,  
                            @RequestParam String destinationBucketName, @RequestParam String destinationKey) {  
    ossTemplate.copyObject(sourceBucketName, sourceKey, destinationBucketName, destinationKey);  
    return R.ok("Object copied from " + sourceKey + " to " + destinationKey);  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/e62bec521883d7ed3a6e0453b1224b24.png)

### 查询指定文件的访问权限
```java
@GetMapping("/getObjectAcl")  
public R<String> getObjectAcl(@RequestParam String bucketName, @RequestParam String objectName) {  
    return R.ok(ossTemplate.getObjectAcl(bucketName, objectName).toString());  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/6add466e7176202f2e492e56c4dbb769.png)

### 设置指定文件的访问权限
```java
@PostMapping("/setObjectAcl")  
public R<String> setObjectAcl(@RequestParam String bucketName, @RequestParam String objectName, @RequestParam String acl) {  
    ossTemplate.setObjectAcl(bucketName, objectName, acl);  
    return R.ok("ACL set for object: " + objectName);  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/423026e4197983c57d2b3993e0af6341.png)

### 启用或者关闭指定bucket版本控制
```java
@PostMapping("/setBucketVersioning")  
public R<String> setBucketVersioning(@RequestParam String bucketName, @RequestParam boolean enable) {  
    ossTemplate.setBucketVersioning(bucketName, enable);  
    return R.ok("Versioning set to " + (enable ? "Enabled" : "Suspended") + " for bucket: " + bucketName);  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/5716ca557c79d1ffa951f74ebe097d2e.png)

### 给指定文件打标签
```java
@PostMapping("/setObjectTags")  
public R<String> setObjectTags(@RequestParam String bucketName, @RequestParam String objectName, @RequestBody Map<String, String> tags) {  
    ossTemplate.setObjectTags(bucketName, objectName, tags);  
    return R.ok("Tags set for object: " + objectName);  
}
```

![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/ec14dfaefa4fcb004bde3422d8a69447.png)

### 获取指定文件的标签
```java
@GetMapping("/getObjectTags")  
public R<Map<String, String>> getObjectTags(@RequestParam String bucketName, @RequestParam String objectName) {  
    return R.ok(ossTemplate.getObjectTags(bucketName, objectName));  
}
```
![image.png](https://nas.allbs.cn:9006/cloudpic/2024/08/e28377d2dfbdb14129e33c8cbd0f962e.png)
