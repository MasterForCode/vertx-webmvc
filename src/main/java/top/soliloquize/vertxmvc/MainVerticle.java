package top.soliloquize.vertxmvc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import top.soliloquize.vertxmvc.spring.SpringUtils;

/**
 * @author wb
 * @date 2019/9/27
 */
public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        SpringUtils.init("classpath:applicationContext.xml");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        HttpServerVerticle.deploy(vertx, new JsonObject().put("server.instance", Runtime.getRuntime().availableProcessors()).put("server.port", 80));
    }
}
