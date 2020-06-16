package top.soliloquize.vertxmvc;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.thymeleaf.impl.ThymeleafTemplateEngineImpl;
import lombok.extern.slf4j.Slf4j;
import top.soliloquize.vertxmvc.core.AppContext;
import top.soliloquize.vertxmvc.core.ConfigOptions;
import top.soliloquize.vertxmvc.core.RouterWrapper;
import top.soliloquize.vertxmvc.core.SupportedTemplateEngine;

/**
 * @author wb
 * @date 2020/6/4
 */
@Slf4j
public class HttpServerVerticle {
    private Vertx vertx;

    public HttpServerVerticle(Vertx vertx) {
        AppContext.INSTANCE.setVertx(vertx);
        this.vertx = vertx;
    }

    public void deploy(JsonObject config) {
        assert config != null;
        String moduleName = config.getString(ConfigOptions.MODULE_NAME);
        Integer instances = config.getInteger(ConfigOptions.SERVER_INSTANCE);
        Integer port = config.getInteger(ConfigOptions.SERVER_PORT);
        String templateEngineName = config.getString(ConfigOptions.TEMPLATE_ENGINE_NAME);

        Router rootRouter = Router.router(this.vertx);
        new RouterWrapper(vertx, moduleName, this.getTemplateEngineByName(templateEngineName)).routerMapping(rootRouter);

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

    private TemplateEngine getTemplateEngineByName(String name) {
        switch (name) {
            case SupportedTemplateEngine.THYMELEAF:
                return new ThymeleafTemplateEngineImpl(vertx);
//            case SupportedTemplateEngine.JSP:
//                return new ThymeleafTemplateEngineImpl(vertx);
            default:
                return null;
        }
    }
}
