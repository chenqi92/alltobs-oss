package cn.allbs.oss.annotation;

import cn.allbs.oss.OssAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 注解 EnableAllbsOss
 *
 * @author ChenQi
 * &#064;date  2024/4/7
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({OssAutoConfiguration.class})
public @interface EnableAllbsOss {
}
