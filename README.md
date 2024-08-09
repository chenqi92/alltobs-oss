## 依赖jar包
| 引入包             | 版本       |
| ------------------ |----------|
| jdk                | 17       |
| spring boot        | 3.3.42   |
| spring-boot-autoconfigure | 3.3.42   |
| aws-java-sdk-s3 | 1.12.767 |
| jakarta.validation-api | 3.1.0    |

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



### 添加配置
```yaml
oss:
  endpoint: http://xxx.xxx.xxx.xxx:9000
  # 所在地区
  region: com-north-1
  # minio账号或者
  access-key: adadmin
  # 密码
  secret-key: 123456778
  # 设置一个默认的文件桶，比如不同项目使用同一个文件库，以项目为文件桶分隔
  bucket-name: test
  # 设置会过期的子文件桶
  expiring-buckets:
    temp-bucket-1: 30  # 30 天后自动删除
    temp-bucket-2: 60  # 60 天后自动删除
```

### 上传

#### 代码示例

```java
@PostMapping("/upload")
public R upload(@RequestParam("file") MultipartFile file, String folder) {
    folder = Optional.ofNullable(folder).orElse("folder");
    String fileName = file.getOriginalFilename();
    // 文件夹不存在创建
    Map<String, Object> resultMap = new HashMap<>(4);
    resultMap.put("fileName", fileName);
    resultMap.put("url", String.format("/%s/%s", folder, fileName));

    try {
        ossTemplate.createBucket(folder);
        ossTemplate.putObject(folder, fileName, file.getInputStream());
        // el-upload 展示需要
        resultMap.put("name", file.getOriginalFilename());
    } catch (Exception e) {
        log.error("上传失败", e);
        return R.fail(e.getLocalizedMessage());
    }
    return R.ok(resultMap);
}
```

#### 调用

![image-20221104152152404](https://nas.allbs.cn:9006/cloudpic/2022/11/66bb58536c4c56ac2c6c7087dbf7ad22.png)

#### 效果

![image-20221104152336879](https://nas.allbs.cn:9006/cloudpic/2022/11/ede25c893bc689532442d4a2e403b431.png)

### 删除

#### 代码示例

```java
@ApiOperation(value = "通过文件名称删除文件管理", notes = "通过文件名称删除文件管理")
@DeleteMapping("/deleteFile")
public R removeById(@RequestParam("name") String name, @RequestParam("folder") String folder) throws Exception {
    folder = Optional.ofNullable(folder).orElse("folder");
    ossTemplate.removeObject(folder, name);
    return R.ok();
}
```

#### 调用

![image-20221104153047028](https://nas.allbs.cn:9006/cloudpic/2022/11/7d73ba0d319c25dae08f8a193a17e7f3.png)

### 读取

#### 代码示例

```java
@GetMapping("/{bucket}/{fileName}")
@ApiOperation(value = "文件读取", notes = "文件读取")
public void file(@PathVariable String bucket, @PathVariable String fileName, HttpServletResponse response) {
    try (S3Object s3Object = ossTemplate.getObject(bucket, fileName)) {
        response.setContentType("application/octet-stream; charset=UTF-8");
        IoUtil.copy(s3Object.getObjectContent(), response.getOutputStream());
    } catch (Exception e) {
        log.error("文件读取异常: {}", e.getLocalizedMessage());
    }
}
```

#### 调用

![image-20221104163020822](https://nas.allbs.cn:9006/cloudpic/2022/11/d8561042d493301a13ec1d34ffc93a12.png)

### 获取文件url

#### 代码示例

```java
@GetMapping("/{bucket}/{fileName}")
@ApiOperation(value = "文件读取", notes = "文件读取")
public R<String> fileUrl(@PathVariable String bucket, @PathVariable String fileName) {
    return R.ok(ossTemplate.getObjectURL(bucket, fileName));
}
```

#### 调用

![image-20221104163415288](https://nas.allbs.cn:9006/cloudpic/2022/11/55a58a1f7de0473eccb529cae309e6e3.png)

### 获取文件url带过期时间

#### 代码示例

```java
@GetMapping("/{bucket}/{fileName}")
@ApiOperation(value = "文件读取", notes = "文件读取")
public R<String> fileUrl(@PathVariable String bucket, @PathVariable String fileName) {
    // 文件十小时过期示例
    return R.ok(ossTemplate.getObjectURL(bucket, fileName, Duration.ofHours(10)));
}
```

#### 调用

![image-20221104163557607](https://nas.allbs.cn:9006/cloudpic/2022/11/218d811d98630a80adcf9d2c1166ed57.png)

### 文件批量上传

#### 代码示例

```java
@ApiOperation(value = "批量文件上传", notes = "文件批量上传")
@PostMapping(value = "/fileUpload", headers = "content-type=multipart/form-data")
public R save(@Valid @RequestParam(value = "files") MultipartFile[] files, @RequestParam("folder") String folder) {
    if (files.length == 0) {
        return R.fail("文件内容不能为空");
    }
    try {
        // 获取文件夹名称
        for (MultipartFile file : files) {
            ossTemplate.putObject(folder, IdUtil.simpleUUID() + StrUtil.DOT + FileUtil.extName(file.getOriginalFilename()), file.getInputStream());
        }
    } catch (Exception e) {
        return R.fail(e.getLocalizedMessage());
    }
    return R.ok();
}
```

#### 调用

![image-20221104164137648](https://nas.allbs.cn:9006/cloudpic/2022/11/d2401df201a12ad5981a00a1d4867f70.png)

#### 效果

![image-20221104170510271](https://nas.allbs.cn:9006/cloudpic/2022/11/8e922c266339c8541e674fcfe5f868e8.png)

### 云服务商oss服务示例

#### 上传、下载、预览等代码同上方

#### 添加配置

![image-20221104172637827](https://nas.allbs.cn:9006/cloudpic/2022/11/397d1f2207dcf632571396489b75f583.png)

##### endpoint

![image-20221104172722083](https://nas.allbs.cn:9006/cloudpic/2022/11/0c8c5e2f6d424fb651f1197909373982.png)

##### access-key和secret-key

![image-20221104172841532](https://nas.allbs.cn:9006/cloudpic/2022/11/040ddf6ec8a735bfc296564d439ba744.png)

##### bucket-name

可不设置，如果需要一个基础文件夹则设置这个属性

#### 效果

![image-20221104173010493](https://nas.allbs.cn:9006/cloudpic/2022/11/220ac8ae5635a2453893c14ecb3f0204.png)
