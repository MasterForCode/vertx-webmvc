package top.soliloquize.vertxmvc.core;

import io.vertx.core.json.JsonObject;
import lombok.Builder;
import lombok.Data;

/**
 * 配置中心
 * @author wb
 * @date 2020/1/18
 */
@Data
@Builder
public class MvcConfig {
    @Builder.Default
    private JsonObject jsonObject = new JsonObject()
            .putNull("module.name")
            .put("server.instance", Runtime.getRuntime().availableProcessors())
            .put("server.port", 80);
    private ViewResolver viewResolver;
}
