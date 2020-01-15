package top.soliloquize.vertxmvc.core;

import com.google.common.primitives.Primitives;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.soliloquize.vertxmvc.enums.ControllerEnum;
import top.soliloquize.vertxmvc.exceptions.AnnotationException;
import top.soliloquize.vertxmvc.exceptions.ControllerException;
import top.soliloquize.vertxmvc.exceptions.MappingException;
import top.soliloquize.vertxmvc.spring.SpringUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class RouterWrapper {
    private static final String PATH_SPLIT = "/";
    private static final Object[] REQUEST_METHOD = new Object[]{RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class};

    /**
     * 检测request方法是否被正确注解
     *
     * @param method 方法
     * @return Boolean
     */
    private static boolean validatorRequest(Method method) {
        List<String> leftList = Arrays.stream(method.getAnnotations()).map(each -> each.annotationType().toString()).collect(Collectors.toList());
        List<Object> objects = new ArrayList<>();
        for (Object r : RouterWrapper.REQUEST_METHOD) {
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

    private static void validatorParameter(String methodName, Parameter[] parameters) {
        long requestBodyCount = Arrays.stream(parameters).filter(parameter -> parameter.getAnnotation(RequestBody.class) != null).count();
        if (requestBodyCount > 1) {
            throw new AnnotationException("method " + methodName + " has too many @RequestBody annotation");
        }
        for (Parameter parameter : parameters) {
            if (requestBodyCount == 1 && RouterWrapper.isStringOrPrimitiveType(parameter.getType())) {
                throw new AnnotationException("parameter " + parameter.getName() + " can not has @RequestBody annotation");
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
     * 获得所有控制器
     *
     * @return 控制器
     */
    private Map<ControllerEnum, Map<String, Object>> getAllController() {
        Map<String, Object> controllerMap = SpringUtils.applicationContext.getBeansWithAnnotation(Controller.class);
        Map<String, Object> restControllerMap = SpringUtils.applicationContext.getBeansWithAnnotation(RestController.class);
        controllerMap.forEach((name, controller) -> {
            if (ArrayUtils.contains(controller.getClass().getAnnotations(), RestController.class)) {
                throw new ControllerException("controller " + name + " has two kind annotation");
            }

        });
        restControllerMap.forEach((name, controller) -> {
            if (ArrayUtils.contains(controller.getClass().getAnnotations(), Controller.class)) {
                throw new ControllerException("controller " + name + " has two kind annotation");
            }

        });
        Map<ControllerEnum, Map<String, Object>> map = new HashMap<>(2);
        map.put(ControllerEnum.CONTROLLER, controllerMap);
        map.put(ControllerEnum.REST_CONTROLLER, restControllerMap);
        return map;
    }

    /**
     * 为根root配置路由
     *
     * @param rootRouter 根root
     */
    public void routerMapping(Router rootRouter) {
        // 获得所有的controller
        Map<ControllerEnum, Map<String, Object>> allControllerMap = getAllController();
        if (allControllerMap.get(ControllerEnum.CONTROLLER).size() == 0 && allControllerMap.get(ControllerEnum.REST_CONTROLLER).size() == 0) {
            log.warn("no controller found");
            return;
        }
        Map<String, Object> controllerMap = allControllerMap.get(ControllerEnum.CONTROLLER);
        Map<String, Object> restControllerMap = allControllerMap.get(ControllerEnum.REST_CONTROLLER);
        if (controllerMap != null) {
            // 处理所有控制器
            controllerMap.forEach((name, controller) -> {
                // 获取根path
                String rootPath = getRootPath(controller);
                // 处理控制器中的方法，过滤掉没有@RequestMapping、@GetMapping、@PostMapping、@PutMapping、@DeleteMapping任意一个注解的方法
                // 并限制只有一种注解
                List<Method> methods = Arrays.stream(controller.getClass().getDeclaredMethods())
                        .filter(RouterWrapper::validatorRequest)
                        .collect(Collectors.toList());
                if (methods.size() > 0) {
                    // 处理方法
                    for (Method method : methods) {
                        actionMethod(rootRouter, rootPath, controller, method);
                    }
                }
            });
        }
    }

    private void actionMethod(Router rootRouter, String rootPath, Object controller, Method method) {
        // 获取方法上的path
        String methodPath = getMethodPath(method);
        Handler<RoutingContext> requestHandler = ctx -> {
            Object[] args = actionParameter(method, ctx);
            HttpServerResponse response = ctx.response();
            Object result = null;
            try {
//                result = MethodHandles.lookup().unreflect(method).bindTo(controller).invokeWithArguments(args);
                result = method.invoke(controller, args);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            if (!response.headWritten()) {
                response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            }

            if (ctx.failed()) {
                log.error("request error, {}::{}", controller.getClass().getName(), method.getName(), ctx.failure());
                response.end(Json.encode(ReturnBean.builder().code(ctx.statusCode()).msg(ctx.failure().getMessage()).build()));
            } else {
                if (method.getReturnType() == void.class) {
                    response.end(Json.encode(ReturnBean.builder().build()));
                }
                response.end(Json.encode(ReturnBean.builder().data(result).build()));
            }
        };
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestMapping) {
                RequestMethod[] httpMethods = ((RequestMapping) annotation).method();
                if (httpMethods.length == 0) {
                    rootRouter.route(rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
                } else {
                    for (RequestMethod httpMethod : httpMethods) {
                        rootRouter.route(getHttpMethod(httpMethod), rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
                    }
                }
            }
            if (annotation instanceof GetMapping) {
                rootRouter.route(HttpMethod.GET, rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
            }
            if (annotation instanceof PostMapping) {
                rootRouter.route(HttpMethod.POST, rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
            }
            if (annotation instanceof PutMapping) {
                rootRouter.route(HttpMethod.PUT, rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
            }
            if (annotation instanceof DeleteMapping) {
                rootRouter.route(HttpMethod.DELETE, rootPath + methodPath).handler(BodyHandler.create()).handler(requestHandler);
            }
        }

    }

    private Object[] actionParameter(Method method, RoutingContext rc) {
        Parameter[] parameters = method.getParameters();
        String[] params = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        Object[] args = new Object[parameters.length];
        RouterWrapper.validatorParameter(method.getName(), parameters);
        if (parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() == RoutingContext.class) {
                    args[i] = rc;
                } else if (parameters[i].getType().isArray() || Collection.class.isAssignableFrom(parameters[i].getType()) || isStringOrPrimitiveType(parameters[i].getType())) {
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    args[i] = parseSimpleTypeOrArrayOrCollection(rc.request().params(), parameters[i].getType(), params[i], genericParameterTypes[i]);
                } else {
                    args[i] = parseBeanType(rc.request().params(), parameters[i].getType());
                }
            }
        }
        return args;
    }

    /**
     * 获取控制器上的根path
     *
     * @param controller 控制器
     * @return 根path
     */
    private String getRootPath(Object controller) {
        String rootPath = "";
        RequestMapping rootRequestMapping = controller.getClass().getAnnotation(RequestMapping.class);
        if (rootRequestMapping != null) {
            // 根path
            String[] values = rootRequestMapping.value();
            rootPath = pathAction(values);
        }
        return rootPath;
    }

    /**
     * 处理requestMapping中指定的path
     *
     * @param values 所有path
     * @return 处理后的path
     */
    private String pathAction(String[] values) {
        String path;
        if (values.length > 0) {
            path = String.join(RouterWrapper.PATH_SPLIT, values);
            if (!path.startsWith(RouterWrapper.PATH_SPLIT)) {
                path = RouterWrapper.PATH_SPLIT + path;
            }
        } else {
            path = RouterWrapper.PATH_SPLIT;
        }
        return path;
    }

    /**
     * 获取方法上的path
     *
     * @param method Method
     * @return path
     */
    private String getMethodPath(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);

        Object[] mappings = new Object[]{requestMapping, getMapping, postMapping, putMapping, deleteMapping};
        int count = 0;
        for (Object obj : mappings) {
            if (obj != null) {
                count++;
            }
        }
        if (count != 1) {
            throw new MappingException(method.getName() + "'s mappings number is wrong, only one here");
        }
        if (requestMapping != null) {
            String[] path = requestMapping.value();
            return pathAction(path);
        }
        if (getMapping != null) {
            String[] path = getMapping.value();
            return pathAction(path);
        }
        if (postMapping != null) {
            String[] path = postMapping.value();
            return pathAction(path);
        }
        if (putMapping != null) {
            String[] path = putMapping.value();
            return pathAction(path);
        }
        if (deleteMapping != null) {
            String[] path = deleteMapping.value();
            return pathAction(path);
        }
        return "";
    }

    /**
     * RequestMethod转vertx的HttpMethod
     *
     * @param requestMethod requestMethod请求方式
     * @return HttpMethod
     */
    private HttpMethod getHttpMethod(RequestMethod requestMethod) {
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

    private Object parseSimpleTypeOrArrayOrCollection(MultiMap allParams, Class<?> paramType, String paramName, Type genericParameterTypes) {

        if (paramType.isArray()) {

            Class<?> componentType = paramType.getComponentType();

            List<String> values = allParams.getAll(paramName);
            Object array = Array.newInstance(componentType, values.size());
            for (int j = 0; j < values.size(); j++) {
                Array.set(array, j, parseSimpleType(values.get(j), componentType));
            }
            return array;
        }
        // Collection type
        else if (Collection.class.isAssignableFrom(paramType)) {
            try {
                return parseCollectionType(allParams.getAll(paramName), genericParameterTypes);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        // String and primitive type
        else if (isStringOrPrimitiveType(paramType)) {
            return parseSimpleType(allParams.get(paramName), paramType);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T parseSimpleType(String value, Class<T> targetClass) {
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


    private Collection parseCollectionType(List<String> values, Type genericParameterType) throws Throwable {
        Class<?> actualTypeArgument = String.class;
        Class<?> rawType;
        if (genericParameterType instanceof ParameterizedType) {
            ParameterizedType parameterType = (ParameterizedType) genericParameterType;
            actualTypeArgument = (Class<?>) parameterType.getActualTypeArguments()[0];
            rawType = (Class<?>) parameterType.getRawType();
        } else {
            rawType = (Class<?>) genericParameterType;
        }

        Collection coll;
        if (rawType == List.class) {
            coll = new ArrayList<>();
        } else if (rawType == Set.class) {
            coll = new HashSet<>();
        } else {
            coll = (Collection) rawType.newInstance();
        }

        for (String value : values) {
            coll.add(parseSimpleType(value, actualTypeArgument));
        }
        return coll;
    }


    private Object parseBeanType(MultiMap allParams, Class<?> paramType) {
        Object bean = null;
        try {
            bean = paramType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        Field[] fields = paramType.getDeclaredFields();
        for (Field field : fields) {
            Object value = parseSimpleTypeOrArrayOrCollection(allParams, field.getType(), field.getName(), field.getGenericType());

            field.setAccessible(true);
            try {
                field.set(bean, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return bean;
    }

}
