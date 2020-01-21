package top.soliloquize.vertxmvc.filter;

import io.vertx.ext.web.RoutingContext;
import org.springframework.stereotype.Component;
import top.soliloquize.vertxmvc.core.Filter;


/**
 * @author wb
 * @date 2020/1/19
 */
@Component
public class BFilter extends Filter {


    @Override
    public void doFilter(RoutingContext rc) {
        super.urlPattern = "/*";
        super.order = 2L;
        System.out.println("--b---------");
    }
}
