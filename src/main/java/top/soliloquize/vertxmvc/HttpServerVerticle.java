package top.soliloquize.vertxmvc;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import top.soliloquize.vertxmvc.core.MvcConfig;
import top.soliloquize.vertxmvc.core.RouterWrapper;
import top.soliloquize.vertxmvc.core.ViewResolver;

/**
 * 部署vertx应用
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class HttpServerVerticle {

    public static void deploy(Vertx vertx, MvcConfig mvcConfig) {
        String moduleName = mvcConfig.getJsonObject().getString("module.name");
        Integer instances = mvcConfig.getJsonObject().getInteger("server.instance");
        Integer port = mvcConfig.getJsonObject().getInteger("server.port");
        ViewResolver viewResolver = mvcConfig.getViewResolver();
        if (instances == null) {
            instances = Runtime.getRuntime().availableProcessors();
        }
        if (port == null) {
            port = 8080;
        }

        Router rootRouter = Router.router(vertx);
        RouterWrapper routerWrapper = new RouterWrapper(vertx, moduleName, viewResolver);
        routerWrapper.routerMapping(rootRouter);

        // 根据cpu核数或指定的数据部署实例个数
        for (int i = 0; i < instances; ++i) {
            HttpServer server = vertx.createHttpServer();
            server.requestHandler(rootRouter).listen(port, (ar) -> {
                if (ar.failed()) {
                    log.error("Failed to start httpServer", ar.cause());
                    vertx.close();
                    System.exit(1);
                } else {
                    log.info(String.format("Server is listening on port %d", server.actualPort()));
                }

            });
        }

    }
}
