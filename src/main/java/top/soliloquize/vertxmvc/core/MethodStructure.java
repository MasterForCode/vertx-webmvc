package top.soliloquize.vertxmvc.core;

import io.vertx.core.http.HttpMethod;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * method详情
 *
 * @author wb
 * @date 2020/1/16
 */
@Data
@Builder
public class MethodStructure {
    private String path;
    private String methodName;
    private Object[] args;
    private String[] parameters;
    private boolean isFuture;
    private boolean isRestFul;
    private Method method;
    @Builder.Default
    private List<HttpMethod> httpMethod = new ArrayList<>();
}
