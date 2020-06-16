package top.soliloquize.vertxmvc.core;

import io.vertx.ext.web.RoutingContext;
import top.soliloquize.vertxmvc.spring.Springs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 过滤器解析器
 *
 * @author wb
 * @date 2020/1/20
 */
public class InterceptorResolver {
    /**
     * 获取所有的拦截器
     *
     * @return Map<String, HandlerInterceptor>
     */
    public static Map<String, HandlerInterceptor> getInterceptors() {
        return Springs.getApplicationContext().getBeansOfType(HandlerInterceptor.class);
    }

    /**
     * 将拦截器器按order排序
     *
     * @param interceptorMap Map<String, HandlerInterceptor>
     */
    public static List<HandlerInterceptor> sortInterceptor(Map<String, HandlerInterceptor> interceptorMap) {
        return interceptorMap.values().stream()
                .sorted(((o1, o2) -> Math.toIntExact(o1.order - o2.order)))
                .collect(Collectors.toList());
    }

    /**
     * 执行前置拦截器器方法
     *
     * @param handlerInterceptor     过滤器
     * @param rc         routingContext 上下文
     */
    public static boolean actionPreInterceptor(HandlerInterceptor handlerInterceptor, RoutingContext rc) {
       return handlerInterceptor.preInterceptor(rc);
    }

    /**
     * 执行后置拦截器器方法
     *
     * @param handlerInterceptor     过滤器
     * @param rc         routingContext 上下文
     */
    public static void actionAfterInterceptor(HandlerInterceptor handlerInterceptor, RoutingContext rc) {
        handlerInterceptor.afterInterceptor(rc);
    }
}
