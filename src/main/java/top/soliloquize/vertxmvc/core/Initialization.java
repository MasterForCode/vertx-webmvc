package top.soliloquize.vertxmvc.core;


import io.vertx.core.Vertx;
import top.soliloquize.vertxmvc.spring.Springs;

/**
 * @author wb
 * @date 2020/6/5
 */
public interface Initialization {
    /**
     * 初始化spring
     */
    default void initSpring(Vertx vertx) {
        Springs.initByFile("classpath:applicationContext.xml");
    };
}
