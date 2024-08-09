package cn.allbs.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 类 OssProperties
 * </p>
 * oss 配置信息
 *
 * @author ChenQi
 * &#064;date  2022/11/4 13:45
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = OssProperties.PREFIX)
public class OssProperties {

    /**
     * 配置前缀
     */
    public static final String PREFIX = "oss";

    /**
     * 是否启用 oss，默认为：true
     */
    private boolean enable = true;

    /**
     * 对象存储服务的URL
     */
    private String endpoint;

    /**
     * 自定义域名
     */
    private String customDomain;

    /**
     * true path-style nginx 反向代理和S3默认支持 pathStyle {<a href="http://endpoint/bucketname">...</a>} false
     * supports virtual-hosted-style 阿里云等需要配置为 virtual-hosted-style
     * 模式{<a href="http://bucketname.endpoint">...</a>}
     */
    private Boolean pathStyleAccess = true;

    /**
     * 区域
     */
    private String region;

    /**
     * Access key就像用户ID，可以唯一标识你的账户
     */
    private String accessKey;

    /**
     * Secret key是你账户的密码
     */
    private String secretKey;

    /**
     * 默认的存储桶名称
     */
    private String bucketName;

    /**
     * 定义多个子 Bucket 及其存储时间（单位：天）
     * key 为子 Bucket 名称，value 为存储天数
     */
    private Map<String, Integer> expiringBuckets;

}
