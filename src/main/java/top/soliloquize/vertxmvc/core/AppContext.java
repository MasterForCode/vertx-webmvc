package top.soliloquize.vertxmvc.core;

import io.vertx.core.Vertx;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author wb
 * @date 2020/6/16
 */
@Data
@Accessors(chain = true)
public class AppContext {
    public static final AppContext INSTANCE = new AppContext();
    private Vertx vertx;
}
