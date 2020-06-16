package top.soliloquize.vertxmvc.interceptor;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.soliloquize.vertxmvc.core.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * @author wb
 * @date 2020/1/21
 */
@Slf4j
@Component
public class DefaultInterceptor extends HandlerInterceptor {


    public DefaultInterceptor() {
        super(0L,  "/*");
    }

    @Override
    public boolean preInterceptor(RoutingContext rc) {
        rc.put("arrivalDate", System.currentTimeMillis());
        return super.preInterceptor(rc);
    }

    @Override
    public void afterInterceptor(RoutingContext rc) {
        log.info("remote address: "+rc.request().host()+",path: "+rc.request().path()+",spend: " + (System.currentTimeMillis() - (Long)rc.get("arrivalDate")) + "ms");
        super.afterInterceptor(rc);
    }
}
