package top.soliloquize.vertxmvc.core;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * 过滤器
 *
 * @author wb
 * @date 2020/1/18
 */
public abstract class Filter {
    /**
     * 执行优先级
     */
    public long order = 0L;
    /**
     * url匹配后执行filter
     */
    public String urlPattern = "/*";

    /**
     * 过滤器接口
     *
     * @param rc RoutingContext 上下文
     */
    public abstract void doFilter(RoutingContext rc);

}
