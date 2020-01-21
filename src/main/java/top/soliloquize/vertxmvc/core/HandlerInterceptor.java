package top.soliloquize.vertxmvc.core;

import io.vertx.ext.web.RoutingContext;

/**
 * @author wb
 * @date 2020/1/21
 */
public abstract class HandlerInterceptor {
    /**
     * 执行优先级
     */
    public long order = 0L;

    /**
     * url匹配后执行interceptor
     */
    public String urlPattern = "/*";

    public HandlerInterceptor(long order, String urlPattern) {
        this.order = order;
        this.urlPattern = urlPattern;
    }

    /**
     * 拦截器前置处理器
     *
     * @param rc RoutingContext 上下文
     * @return true继续执行
     */
    public boolean preInterceptor(RoutingContext rc) {
        return true;
    };
    /**
     * 拦截器后置处理器
     *
     * @param rc RoutingContext 上下文
     */
    public void afterInterceptor(RoutingContext rc) {};
}
