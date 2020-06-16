package top.soliloquize.vertxmvc.core;

import com.google.common.primitives.Primitives;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.web.bind.annotation.*;
import top.soliloquize.vertxmvc.exceptions.AnnotationException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请求解析器
 * @author wb
 * @date 2020/1/18
 */
@Slf4j
public class RequestResolver {
    public static final Object[] REQUEST_METHOD = new Object[]{
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class
    };

    /**
     * 检测request方法是否被正确注解
     *
     * @param method 方法
     * @return Boolean
     */
    static boolean validatorRequest(Method method) {
        if (method.getName().startsWith(Const.LAMBDA)) {
            return false;
        }
        List<String> leftList = Arrays.stream(method.getAnnotations()).map(each -> each.annotationType().toString()).collect(Collectors.toList());
        List<Object> objects = new ArrayList<>();
        for (Object r : RequestResolver.REQUEST_METHOD) {
            if (leftList.contains(r.toString())) {
                objects.add(r);
            }
        }
        if (objects.size() == 0) {
            log.warn("No annotations on " + method.getName() + " method, and it will be ignored");
            return false;
        }
        if (objects.size() > 1) {
            log.error("Multiple annotations on " + method.getName() + " method, and it will be ignored");
            return false;
        }
        return true;
    }

    /**
     * 检测参数是否合理
     *
     * @param methodName 方法名
     * @param parameters 参数
     */
    static void validatorParameter(String methodName, Parameter[] parameters) {
        long requestBodyCount = Arrays.stream(parameters).filter(parameter -> parameter.getAnnotation(RequestBody.class) != null).count();
        if (requestBodyCount > 1) {
            throw new AnnotationException("method " + methodName + " has too many @RequestBody annotation", new Throwable());
        }
        for (Parameter parameter : parameters) {
            if (requestBodyCount == 1 && RequestResolver.isStringOrPrimitiveType(parameter.getType())) {
                throw new AnnotationException("parameter " + parameter.getName() + " can not has @RequestBody annotation", new Throwable());
            }
        }
    }

    /**
     * 判断是否是基本类型及其包装类型或者是String
     *
     * @param targetClass 测试Class
     * @return 验证结果
     */
    private static boolean isStringOrPrimitiveType(Class<?> targetClass) {
        return targetClass == String.class || Primitives.allWrapperTypes().contains(Primitives.wrap(targetClass));
    }

    /**
     * 注入参数
     *
     * @param method 方法
     * @param rc     routing上下文
     * @return 参数
     */
    static Object[] injectionParameters(Method method, RoutingContext rc) {
        Parameter[] parameters = method.getParameters();
        String[] params = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        Object[] args = new Object[parameters.length];
        RequestResolver.validatorParameter(method.getName(), parameters);
        if (parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() == RoutingContext.class) {
                    args[i] = rc;
                } else if (parameters[i].getType().isArray() || Collection.class.isAssignableFrom(parameters[i].getType()) || isStringOrPrimitiveType(parameters[i].getType())) {
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    args[i] = RequestResolver.parseSimpleTypeOrArrayOrCollection(rc.request().params(), parameters[i].getType(), params[i], genericParameterTypes[i]);
                } else {
                    args[i] = RequestResolver.parseBeanType(rc.request().params(), parameters[i].getType());
                }
            }
        }
        return args;
    }

    /**
     * 处理参数是bean的情况
     *
     * @param allParams 所有参数值
     * @param paramType bean的Class
     * @return bean实例
     */
    private static Object parseBeanType(MultiMap allParams, Class<?> paramType) {
        Object bean;
        try {
            bean = paramType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Instantiation parameter error");
            throw new RuntimeException("Instantiation parameter error");
        }
        Field[] fields = paramType.getDeclaredFields();
        for (Field field : fields) {
            Object value = RequestResolver.parseSimpleTypeOrArrayOrCollection(allParams, field.getType(), field.getName(), field.getGenericType());
            field.setAccessible(true);
            try {
                field.set(bean, value);
            } catch (IllegalAccessException e) {
                log.error("Instantiation parameter error");
                throw new RuntimeException("Instantiation parameter error");
            }
        }
        return bean;
    }

    /**
     * 处理参数是基本类型、数组、或集合的情况
     *
     * @param allParams             所有参数值
     * @param paramType             参数类型Class
     * @param paramName             参数名
     * @param genericParameterTypes 参数Type
     * @return bean实例
     */
    private static Object parseSimpleTypeOrArrayOrCollection(MultiMap allParams, Class<?> paramType, String paramName, Type genericParameterTypes) {
        // 数组
        if (paramType.isArray()) {
            Class<?> componentType = paramType.getComponentType();
            List<String> values = allParams.getAll(paramName);
            Object array = Array.newInstance(componentType, values.size());
            for (int j = 0; j < values.size(); j++) {
                Array.set(array, j, RequestResolver.parseSimpleType(values.get(j), componentType));
            }
            return array;
        }
        // 集合
        else if (Collection.class.isAssignableFrom(paramType)) {
            try {
                return RequestResolver.parseCollectionType(allParams.getAll(paramName), genericParameterTypes);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        // String和基本类型
        else if (isStringOrPrimitiveType(paramType)) {
            return RequestResolver.parseSimpleType(allParams.get(paramName), paramType);
        }

        return null;
    }

    /**
     * 处理参数是集合的情况
     *
     * @param values               参数值
     * @param genericParameterType 参数Type
     * @return 实例化的集合
     */
    private static Collection<?> parseCollectionType(List<String> values, Type genericParameterType) {
        Class<?> actualTypeArgument = String.class;
        Class<?> rawType;
        if (genericParameterType instanceof ParameterizedType) {
            ParameterizedType parameterType = (ParameterizedType) genericParameterType;
            actualTypeArgument = (Class<?>) parameterType.getActualTypeArguments()[0];
            rawType = (Class<?>) parameterType.getRawType();
        } else {
            rawType = (Class<?>) genericParameterType;
        }

        Collection<?> collection;
        if (rawType == List.class) {
            collection = new ArrayList<>();
        } else if (rawType == Set.class) {
            collection = new HashSet<>();
        } else {
            try {
                collection = (Collection<?>) rawType.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                log.error("Instantiation parameter error");
                throw new RuntimeException("Instantiation parameter error");
            }
        }
        for (String value : values) {
            collection.add(RequestResolver.parseSimpleType(value, actualTypeArgument));
        }
        return collection;
    }

    /**
     * String转基本类型
     *
     * @param value       值
     * @param targetClass 基本类型的包装类型的Class
     * @param <T>         基本类型的包装类型
     * @return 转换后的值
     */
    private static <T> T parseSimpleType(String value, Class<?> targetClass) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Class<?> wrapType = Primitives.wrap(targetClass);
        if (Primitives.allWrapperTypes().contains(wrapType)) {
            try {
                MethodHandle valueOf = MethodHandles.lookup().unreflect(wrapType.getMethod("valueOf", String.class));
                return (T) valueOf.invoke(value);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else if (targetClass == String.class) {
            return (T) value;
        }
        return null;
    }

    /**
     * RequestMethod转vertx的HttpMethod
     *
     * @param requestMethod requestMethod请求方式
     * @return HttpMethod
     */
    static HttpMethod getHttpMethod(RequestMethod requestMethod) {
        switch (requestMethod) {
            case GET:
                return HttpMethod.GET;
            case HEAD:
                return HttpMethod.HEAD;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case PATCH:
                return HttpMethod.PATCH;
            case DELETE:
                return HttpMethod.DELETE;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case TRACE:
                return HttpMethod.TRACE;
            default:
                return HttpMethod.OTHER;
        }
    }
}
