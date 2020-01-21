package top.soliloquize.vertxmvc.annotations;

import java.lang.annotation.*;

/**
 * 当方式是阻塞方法时使用该注解
 * @author wb
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Blocking {
}
