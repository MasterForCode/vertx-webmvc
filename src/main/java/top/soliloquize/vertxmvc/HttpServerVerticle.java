package top.soliloquize.vertxmvc;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import top.soliloquize.vertxmvc.core.RouterWrapper;

/**
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class HttpServerVerticle {


    public static void deploy(Vertx vertx, JsonObject jsonObject) {
        Integer instances = jsonObject.getInteger("server.instance");
        Integer port = jsonObject.getInteger("server.port");
        if (instances == null) {
            instances = Runtime.getRuntime().availableProcessors();
        }
        if (port == null) {
            port = 8080;
        }

        Router rootRouter = Router.router(vertx);
        new RouterWrapper().routerMapping(rootRouter);

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
