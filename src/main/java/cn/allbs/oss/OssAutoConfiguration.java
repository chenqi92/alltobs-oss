package cn.allbs.oss;

import cn.allbs.oss.properties.OssProperties;
import cn.allbs.oss.service.OssTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 类 OssAutoConfiguration
 * </p>
 *
 * @author ChenQi
 * &#064;date  2022/11/4 13:48
 * @since 1.0.0
 */
@EnableConfigurationProperties({OssProperties.class})
public class OssAutoConfiguration {

    /**
     * OSS操作模板
     *
     * @return OSS操作模板
     */
    @Bean
    @ConditionalOnMissingBean(OssTemplate.class)
    @ConditionalOnProperty(prefix = OssProperties.PREFIX, name = "enable", havingValue = "true", matchIfMissing = true)
    public OssTemplate ossTemplate(OssProperties properties) {
        return new OssTemplate(properties);
    }
}
