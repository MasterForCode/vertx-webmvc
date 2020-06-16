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
public class FilterResolver {
    /**
     * 获取所有的过滤器
     *
     * @return Map<String, Filter>
     */
    public static Map<String, Filter> getFilters() {
        return Springs.getApplicationContext().getBeansOfType(Filter.class);
    }

    /**
     * 将过滤器按order排序
     *
     * @param filterMap Map<String, Filter>
     */
    public static List<Filter> sortFilter(Map<String, Filter> filterMap) {
        List<Filter> filterList = filterMap.values().stream()
                .sorted(((o1, o2) -> Math.toIntExact(o1.order - o2.order)))
                .collect(Collectors.toList());
        return filterList;
    }

    /**
     * 执行过滤器方法
     *
     * @param filter     过滤器
     * @param rc         routingContext 上下文
     */
    public static void actionFilter(Filter filter, RoutingContext rc) {
        filter.doFilter(rc);
    }
}
