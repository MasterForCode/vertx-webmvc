package top.soliloquize.vertxmvc.core;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Route;
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
import top.soliloquize.vertxmvc.annotations.Blocking;
import top.soliloquize.vertxmvc.exceptions.ControllerException;
import top.soliloquize.vertxmvc.exceptions.MappingException;
import top.soliloquize.vertxmvc.exceptions.ReturnDataException;
import top.soliloquize.vertxmvc.exceptions.TemplateEngineException;
import top.soliloquize.vertxmvc.spring.SpringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * mvc核心实现类
 *
 * @author wb
 * @date 2019/9/27
 */
@Slf4j
public class RouterWrapper {
    /**
     * 视图处理器
     */
    private ViewResolver viewerResolver;
    /**
     * vertx实例
     */
    private Vertx vertx;
    /**
     * 模块名，将被加在请求路径的最前端
     */
    private String moduleName = "";
    /**
     * 过滤器
     */
    private List<Filter> filterList;
    /**
     * 拦截器
     */
    private List<HandlerInterceptor> handlerInterceptorList;

    /**
     * 构造函数
     *
     * @param vertx          vertx实例
     * @param moduleName     模块名
     * @param viewerResolver 视图处理器
     */
    public RouterWrapper(Vertx vertx, String moduleName, ViewResolver viewerResolver) {
        this.vertx = vertx;
        if (StringUtils.isNotBlank(moduleName)) {
            this.moduleName = Const.PATH_SPLIT + moduleName;
        }
        this.viewerResolver = viewerResolver;
    }

    /**
     * 为根root配置路由
     */
    public void routerMapping(Router rootRouter) {
        // 处理过滤器
        this.filterList = FilterResolver.sortFilter(FilterResolver.getFilters());
        // 处理拦截器
        this.handlerInterceptorList = InterceptorResolver.sortInterceptor(InterceptorResolver.getInterceptors());
        // 获得所有的controller
        Map<String, Object> allControllerMap = getAllController();
        if (allControllerMap.size() == 0) {
            log.warn("no controller found");
            return;
        }
        List<ControllerAnalysis> controllerAnalysisList = this.analysisController(allControllerMap);

        controllerAnalysisList.forEach(controllerAnalysis -> this.actionMethod(rootRouter, controllerAnalysis));

    }

    /**
     * 获得所有控制器,忽略同时具有Controller和RestController注解的控制器
     *
     * @return 控制器
     */
    private Map<String, Object> getAllController() {
        Map<String, Object> controllerMap = SpringUtils.getApplicationContext().getBeansWithAnnotation(Controller.class);
        Map<String, Object> restControllerMap = SpringUtils.getApplicationContext().getBeansWithAnnotation(RestController.class);
        controllerMap.forEach((name, controller) -> {
            if (ArrayUtils.contains(controller.getClass().getAnnotations(), RestController.class)) {
                log.error("controller " + name + " has two kind annotation：[Controller,RestController]");
                throw new ControllerException("controller " + name + " has two kind annotation：[Controller,RestController]");
            }

        });
        restControllerMap.forEach((name, controller) -> {
            if (ArrayUtils.contains(controller.getClass().getAnnotations(), Controller.class)) {
                log.error("controller " + name + " has two kind annotation：[Controller,RestController]");
                throw new ControllerException("controller " + name + " has two kind annotation：[Controller,RestController]");
            }

        });
        Map<String, Object> map = new HashMap<>(2);
        map.putAll(controllerMap);
        map.putAll(restControllerMap);
        return map;
    }

    /**
     * 解析controller
     *
     * @param allControllerMap controller map
     * @return 解析结果
     */
    private List<ControllerAnalysis> analysisController(Map<String, Object> allControllerMap) {
        List<ControllerAnalysis> result = new ArrayList<>(allControllerMap.size());
        allControllerMap.forEach((controllerName, controller) -> {
            ControllerAnalysis element = ControllerAnalysis.builder().controller(controller).controllerName(controllerName).build();
            result.add(element);
            element.setPath(getRootPath(controller));
            // 处理控制器中的方法，过滤掉没有@RequestMapping、@GetMapping、@PostMapping、@PutMapping、@DeleteMapping任意一个注解的方法
            // 并限制只有一种注解
            List<Method> methods = Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(RequestResolver::validatorRequest)
                    .collect(Collectors.toList());
            boolean restFulController = controller.getClass().getAnnotation(RestController.class) != null;
            methods.forEach(method -> {
                MethodAnalysis subElement = MethodAnalysis.builder().method(method).methodName(method.getName()).build();
                element.getMethodAnalysisList().add(subElement);
                subElement.setFuture(Future.class.isAssignableFrom(method.getReturnType()));
                subElement.setPath(this.getMethodPath(method));
                subElement.setRestFul(restFulController || method.getAnnotation(ResponseBody.class) != null);
                subElement.setParameters(new LocalVariableTableParameterNameDiscoverer().getParameterNames(method));

                Parameter[] parameters = method.getParameters();
                subElement.setArgs(new Object[parameters.length]);
                RequestResolver.validatorParameter(method.getName(), parameters);
                // 处理path
                String path = "";
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                if (requestMapping != null) {
                    path += StringUtils.join(requestMapping.value(), Const.PATH_SPLIT);
                    RequestMethod[] requestMethods = requestMapping.method();
                    if (requestMethods.length == 0) {
                        // 默认支持get\post
                        subElement.getHttpMethod().add(HttpMethod.GET);
                        subElement.getHttpMethod().add(HttpMethod.POST);
                    } else {
                        Arrays.stream(requestMethods).forEach(each -> subElement.getHttpMethod().add(RequestResolver.getHttpMethod(each)));
                    }

                }
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                if (getMapping != null) {
                    subElement.getHttpMethod().add(HttpMethod.GET);
                    path += StringUtils.join(getMapping.value(), Const.PATH_SPLIT);
                }
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if (postMapping != null) {
                    subElement.getHttpMethod().add(HttpMethod.POST);
                    path += StringUtils.join(postMapping.value(), Const.PATH_SPLIT);
                }
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                if (putMapping != null) {
                    subElement.getHttpMethod().add(HttpMethod.PUT);
                    path += StringUtils.join(putMapping.value(), Const.PATH_SPLIT);
                }
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                if (deleteMapping != null) {
                    subElement.getHttpMethod().add(HttpMethod.DELETE);
                    path += StringUtils.join(deleteMapping.value(), Const.PATH_SPLIT);
                }
                if (!path.startsWith(Const.PATH_SPLIT)) {
                    path = Const.PATH_SPLIT + path;
                }
                subElement.setPath(path);
            });

        });
        return result;
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
            path = String.join(Const.PATH_SPLIT, values);
            if (!path.startsWith(Const.PATH_SPLIT)) {
                path = Const.PATH_SPLIT + path;
            }
        } else {
            path = Const.PATH_SPLIT;
        }
        return path;
    }

    /**
     * 处理方法
     *
     * @param rootRouter         根路由
     * @param controllerAnalysis 控制器
     */
    private void actionMethod(Router rootRouter, ControllerAnalysis controllerAnalysis) {
        controllerAnalysis.getMethodAnalysisList().forEach(methodAnalysis -> {
            String path = this.moduleName + controllerAnalysis.getPath() + methodAnalysis.getPath();
            Handler<RoutingContext> handler = rc -> {
                methodAnalysis.setArgs(RequestResolver.injectionParameters(methodAnalysis.getMethod(), rc));
                Future<Object> result = methodInvoke(methodAnalysis, controllerAnalysis.getController());
                result.setHandler(re -> {
                    if (re.succeeded()) {
                        AsyncResult<Object> asyncResult = re;
                        if (methodAnalysis.isFuture()) {
                            ((Future<Object>) re.result()).setHandler(r -> {
                                if (r.succeeded()) {
                                    dataAction(rc, methodAnalysis, r.result());
                                } else {
                                    log.error("Method: " + methodAnalysis.getMethodName() + " under the controller: " + controllerAnalysis.getControllerName() + " execute error");
                                    throw new RuntimeException(result.cause());
                                }
                            });
                        } else {
                            dataAction(rc, methodAnalysis, asyncResult.result());
                        }
                    } else {
                        log.error("Method: " + methodAnalysis.getMethodName() + " under the controller: " + controllerAnalysis.getControllerName() + " execute error");
                        throw new RuntimeException(result.cause());
                    }
                });
            };

            actionRoute(rootRouter, methodAnalysis.getHttpMethod(), path, handler);
        });

    }

    /**
     * 执行方法
     *
     * @param methodAnalysis 方法
     * @param controller     控制器
     * @return 结果
     */
    private Future<Object> methodInvoke(MethodAnalysis methodAnalysis, Object controller) {
        Supplier<Object> methodInvokeSupplier = () -> {
            try {
                return methodAnalysis.getMethod().invoke(controller, methodAnalysis.getArgs());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
        Blocking blocking = methodAnalysis.getMethod().getAnnotation(Blocking.class);
        if (blocking != null) {
            // 异步执行阻塞方法
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
     * 处理返回值
     *
     * @param rc             routingContext 上下文
     * @param methodAnalysis 方法详情
     * @param data           返回值
     */
    private void dataAction(RoutingContext rc, MethodAnalysis methodAnalysis, Object data) {
        Class<?> methodReturnType = methodAnalysis.getMethod().getReturnType();
        HttpServerResponse response = rc.response();
        if (methodAnalysis.isRestFul()) {
            response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            if (methodReturnType == void.class) {
                response.end(Json.encode(ReturnBean.builder().build()));
            }
            // 执行后置拦截器
            this.handlerInterceptorList.forEach(each -> {
                String urlPattern = each.urlPattern;
                if (urlPattern.endsWith(Const.ANY)) {
                    if (rc.currentRoute().getPath().startsWith(StringUtils.substring(urlPattern, 0, urlPattern.length() - 1))) {
                        InterceptorResolver.actionAfterInterceptor(each, rc);
                    }
                } else {
                    if (urlPattern.equalsIgnoreCase(rc.currentRoute().getPath())) {
                        InterceptorResolver.actionAfterInterceptor(each, rc);
                    }
                }
            });
            response.end(Json.encode(ReturnBean.builder().data(data).build()));
        } else {
            if (this.viewerResolver != null && this.viewerResolver.getTemplateEngine() != null) {
                if (data instanceof String) {
                    this.viewerResolver.getTemplateEngine().render(rc.data(), (String) data, re -> {
                        if (re.succeeded()) {
                            // 执行后置拦截器
                            this.handlerInterceptorList.forEach(each -> {
                                String urlPattern = each.urlPattern;
                                if (urlPattern.endsWith(Const.ANY)) {
                                    if (rc.currentRoute().getPath().startsWith(StringUtils.substring(urlPattern, 0, urlPattern.length() - 1))) {
                                        InterceptorResolver.actionAfterInterceptor(each, rc);
                                    }
                                } else {
                                    if (urlPattern.equalsIgnoreCase(rc.currentRoute().getPath())) {
                                        InterceptorResolver.actionAfterInterceptor(each, rc);
                                    }
                                }
                            });
                            response.putHeader(HttpHeaders.CONTENT_TYPE, "text/html").end(re.result());
                        } else {
                            rc.fail(re.cause());
                        }
                    });
                } else {
                    log.error("Must return string when using template");
                    throw new ReturnDataException("Must return string when using template");
                }
            } else {
                log.error("No template engine is set");
                throw new TemplateEngineException("No template engine is set");
            }
        }

    }

    /**
     * 为方法配置路由
     *
     * @param router  根路由
     * @param path    路由路径
     * @param handler 路由处理器
     */
    private void actionRoute(Router router, List<HttpMethod> httpMethods, String path, Handler<RoutingContext> handler) {
        Route route = router.route(path);
        httpMethods.forEach(route::method);
        route.handler(BodyHandler.create()).handler(rc -> {
            // 执行过滤器
            this.filterList.forEach(each -> {
                String urlPattern = each.urlPattern;
                if (urlPattern.endsWith(Const.ANY)) {
                    if (route.getPath().startsWith(StringUtils.substring(urlPattern, 0, urlPattern.length() - 1))) {
                        FilterResolver.actionFilter(each, rc);
                    }
                } else {
                    if (urlPattern.equalsIgnoreCase(route.getPath())) {
                        FilterResolver.actionFilter(each, rc);
                    }
                }
            });
            rc.next();
        }).handler(rc -> {
            // 执行前置拦截器
            this.handlerInterceptorList.forEach(each -> {
                String urlPattern = each.urlPattern;
                if (urlPattern.endsWith(Const.ANY)) {
                    if (route.getPath().startsWith(StringUtils.substring(urlPattern, 0, urlPattern.length() - 1))) {
                        boolean result = InterceptorResolver.actionPreInterceptor(each, rc);
                        if (!result) {
                            rc.fail(500);
                        }
                    }
                } else {
                    if (urlPattern.equalsIgnoreCase(route.getPath())) {
                        boolean result = InterceptorResolver.actionPreInterceptor(each, rc);
                        if (!result) {
                            rc.fail(500);
                        }
                    }
                }
            });
            System.out.println(rc.get("arrivalDate").toString());
            rc.next();
        }).handler(handler);
    }
}
