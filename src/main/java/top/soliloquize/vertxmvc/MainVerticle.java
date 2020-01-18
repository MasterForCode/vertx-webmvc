package top.soliloquize.vertxmvc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.templ.thymeleaf.impl.ThymeleafTemplateEngineImpl;
import lombok.extern.slf4j.Slf4j;
import top.soliloquize.vertxmvc.core.MvcConfig;
import top.soliloquize.vertxmvc.core.ViewResolver;
import top.soliloquize.vertxmvc.spring.SpringUtils;

/**
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        SpringUtils.init("classpath:applicationContext.xml");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServerVerticle.deploy(
                vertx,
                MvcConfig.builder().viewResolver(ViewResolver.builder().templateEngine(new ThymeleafTemplateEngineImpl(vertx)).build()).build()
        );
    }
}
