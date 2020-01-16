package top.soliloquize.vertxmvc.core;

import com.google.common.primitives.Primitives;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.soliloquize.vertxmvc.annotations.Blocking;
import top.soliloquize.vertxmvc.exceptions.AnnotationException;
import top.soliloquize.vertxmvc.exceptions.ControllerException;
import top.soliloquize.vertxmvc.exceptions.MappingException;
import top.soliloquize.vertxmvc.spring.SpringUtils;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class RouterWrapper {
    private static final String PATH_SPLIT = "/";
    private static final Object[] REQUEST_METHOD = new Object[]{RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class};
    private Vertx vertx;
    /**
     * 模块名
     */
    private String moduleName;

    public RouterWrapper(Vertx vertx, String moduleName) {
        this.vertx = vertx;
        this.moduleName = RouterWrapper.PATH_SPLIT + StringUtils.defaultIfBlank(moduleName, "");
    }

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

    /**
     * 检测方法是否合理
     *
     * @param methodName 方法名
     * @param parameters 参数
     */
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
    private Map<String, Object> getAllController() {
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
        Map<String, Object> map = new HashMap<>(2);
        map.putAll(controllerMap);
        map.putAll(restControllerMap);
        return map;
    }

    /**
     * 为根root配置路由
     */
    public void routerMapping(Router rootRouter, TemplateEngine templateEngine) {
        // 获得所有的controller
        Map<String, Object> allControllerMap = getAllController();
        if (allControllerMap.size() == 0) {
            log.warn("no controller found");
            return;
        }
        // 处理所有控制器
        allControllerMap.forEach((name, controller) -> {
            // 获取根path
            String rootPath = this.moduleName + getRootPath(controller);
            // 处理控制器中的方法，过滤掉没有@RequestMapping、@GetMapping、@PostMapping、@PutMapping、@DeleteMapping任意一个注解的方法
            // 并限制只有一种注解
            List<Method> methods = Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(RouterWrapper::validatorRequest)
                    .collect(Collectors.toList());
            if (methods.size() > 0) {
                // 处理方法
                for (Method method : methods) {
                    actionMethod(rootRouter, rootPath, controller, method, templateEngine);
                }
            }
        });
    }

    /**
     * 执行方法
     *
     * @param rc         routing上下文
     * @param method     方法
     * @param controller 控制器
     * @return 结果
     */
    private Future<Object> methodInvoke(RoutingContext rc, Method method, Object controller) {
        Object[] args = actionParameter(method, rc);
        Supplier<Object> methodInvokeSupplier = () -> {
            try {
                return method.invoke(controller, args);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
        Blocking blocking = method.getAnnotation(Blocking.class);
        if (blocking != null) {
            // 异步执行阻塞方法
            System.out.println("..........");
            return VertxUtils.executeBlockingEx(this.vertx, methodInvokeSupplier);
        } else {
            // 同步执行
            try {
                return Future.succeededFuture(methodInvokeSupplier.get());
            } catch (Throwable throwable) {
                return Future.failedFuture(throwable);
            }
        }
    }

    /**
     * 处理方法
     *
     * @param rootRouter     根路由
     * @param rootPath       controller path
     * @param controller     控制器
     * @param method         方法
     * @param templateEngine 模板引擎
     */
    private void actionMethod(Router rootRouter, String rootPath, Object controller, Method method, TemplateEngine templateEngine) {
        // 获取方法上的path
        String methodPath = getMethodPath(method);
        boolean isFuture = false;
        if (Future.class.isAssignableFrom(method.getReturnType())) {
            isFuture = true;
        }
        boolean finalIsFuture = isFuture;
        Handler<RoutingContext> handler = rc -> {
            Future<Object> result = methodInvoke(rc, method, controller);
            result.setHandler(res -> {
                AsyncResult<Object> asyncResult = res;
                if (finalIsFuture) {
                    ((Future<Object>) res.result()).setHandler(r -> {
                        if (r.succeeded()) {
                            dataAction(rc, templateEngine, method, r.result());
                        } else {
                            log.error("Method: " + method.getName() + " under the controller: " + controller.getClass().getName() + " execute error");
                        }
                    });
                } else {
                    // TODO FIXME  处理没有模板引擎
                    dataAction(rc, templateEngine, method, asyncResult.result());
                }
            });

        };

        Controller ctrl = controller.getClass().getAnnotation(Controller.class);
        if (ctrl != null) {
            // controller
            ResponseBody res = method.getAnnotation(ResponseBody.class);
            if (res != null) {
                actionRoute(rootRouter, method, rootPath + methodPath, handler);
            } else {
                if (templateEngine != null) {
                    actionRoute(rootRouter, method, rootPath + methodPath, handler);
                } else {
                    actionRoute(rootRouter, method, rootPath + methodPath, handler);
                }
            }
        } else {
            // restController
            actionRoute(rootRouter, method, rootPath + methodPath, handler);
        }
    }

    private void dataAction(RoutingContext rc, TemplateEngine templateEngine, Method method, Object data) {
        HttpServerResponse response = rc.response();
        if (templateEngine != null) {
            templateEngine.render(rc.data(), (String) data, re -> {
                if (re.succeeded()) {
                    response.putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(Json.encode(data));
                } else {
                    rc.fail(re.cause());
                }
            });
        } else {
            response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            if (method.getReturnType() == void.class) {
                response.end(Json.encode(ReturnBean.builder().build()));
            }
            response.end(Json.encode(ReturnBean.builder().data(data).build()));
        }

    }

    /**
     * 为方法配置路由
     *
     * @param router  根路由
     * @param method  方法
     * @param path    路由路径
     * @param handler 路由处理器
     */
    private void actionRoute(Router router, Method method, String path, Handler<RoutingContext> handler) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestMapping) {
                RequestMethod[] httpMethods = ((RequestMapping) annotation).method();
                if (httpMethods.length == 0) {
                    router.route(path).handler(BodyHandler.create()).handler(handler);
                } else {
                    for (RequestMethod httpMethod : httpMethods) {
                        router.route(getHttpMethod(httpMethod), path).handler(BodyHandler.create()).handler(handler);
                    }
                }
            }
            if (annotation instanceof GetMapping) {
                router.route(HttpMethod.GET, path).handler(BodyHandler.create()).handler(handler);
            }
            if (annotation instanceof PostMapping) {
                router.route(HttpMethod.POST, path).handler(BodyHandler.create()).handler(handler);
            }
            if (annotation instanceof PutMapping) {
                router.route(HttpMethod.PUT, path).handler(BodyHandler.create()).handler(handler);
            }
            if (annotation instanceof DeleteMapping) {
                router.route(HttpMethod.DELETE, path).handler(BodyHandler.create()).handler(handler);
            }
        }
    }

    /**
     * 注入参数
     *
     * @param method 方法
     * @param rc     routing上下文
     * @return 参数
     */
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
