package top.soliloquize.vertxmvc.core;

/**
 * 常量
 * @author wb
 * @date 2020/1/18
 */
public interface Const {
    /**
     * 请求分隔符
     */
    String PATH_SPLIT = "/";
    /**
     * 匹配所有
     */
    String ANY = "*";
    /**
     * 通用过滤器方法
     */
    String NORMAL_FILTER = "normal.filter";
    /**
     * 请求过滤器方法
     */
    String REQUEST_FILTER = "request.filter";
    /**
     * 响应过滤器方法
     */
    String RESPONSE_FILTER = "response.filter";
}
