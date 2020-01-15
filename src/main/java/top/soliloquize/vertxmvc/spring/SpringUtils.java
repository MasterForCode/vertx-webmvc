package top.soliloquize.vertxmvc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import top.soliloquize.vertxmvc.enums.ControllerEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wb
 * @date 2019/9/27
 */
public class SpringUtils {
    public static ApplicationContext applicationContext;

    public static void init(String path) {
        applicationContext = new FileSystemXmlApplicationContext(path);
    }

    public static <T> T getBean(Class<T> clz) {
        return applicationContext.getBean(clz);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
