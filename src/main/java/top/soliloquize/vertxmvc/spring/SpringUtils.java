package top.soliloquize.vertxmvc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import top.soliloquize.vertxmvc.enums.ControllerEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring工具类
 * @author wb
 * @date 2019/9/27
 */
public class SpringUtils {
    /**
     * spring上下文
     */
    private static ApplicationContext applicationContext;

    /**
     * 初始化spring
     * @param path spring配置文件路径
     */
    public static void init(String path) {
        applicationContext = new FileSystemXmlApplicationContext(path);
    }

    /**
     * 通过类型获取bean
     * @param clz bean Class
     * @param <T> bean类型
     * @return bean实例
     */
    public static <T> T getBean(Class<T> clz) {
        return applicationContext.getBean(clz);
    }

    /**
     * 获取上下文
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
