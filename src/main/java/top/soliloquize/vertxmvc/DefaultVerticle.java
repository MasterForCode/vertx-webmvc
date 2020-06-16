package top.soliloquize.vertxmvc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import top.soliloquize.vertxmvc.core.ConfigOptions;
import top.soliloquize.vertxmvc.core.Initialization;
import top.soliloquize.vertxmvc.core.SupportedTemplateEngine;
import top.soliloquize.vertxmvc.spring.Springs;

/**
 * @author wb
 * @date 2020/6/4
 */
public class DefaultVerticle extends AbstractVerticle {
    private JsonObject config;

    public DefaultVerticle() {
        this.config = new JsonObject()
                .putNull(ConfigOptions.MODULE_NAME)
                .put(ConfigOptions.SERVER_INSTANCE, Runtime.getRuntime().availableProcessors())
                .put(ConfigOptions.SERVER_PORT, 80)
                .put(ConfigOptions.TEMPLATE_ENGINE_NAME, SupportedTemplateEngine.THYMELEAF);
    }

    public void run(JsonObject config, Initialization initialization) {
        Vertx vertx = Vertx.vertx();
        if (config != null) {
            this.config = config;
        }
        if (initialization != null) {
            initialization.initSpring(vertx);
        } else {
            Springs.initByFile("classpath:applicationContext.xml");
        }
        vertx.deployVerticle(new DefaultVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        new HttpServerVerticle(vertx).deploy(this.config);
    }


    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {

    }
}
