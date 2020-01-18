package top.soliloquize.vertxmvc.core;

import io.vertx.ext.web.common.template.TemplateEngine;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 视图解析器
 *
 * @author wb
 * @date 2020/1/18
 */
@Data
@Builder
public class ViewResolver{
    private static String TEMPLATE_NAME_THYMELEAF = "thymeleaf";
    @Builder.Default
    private String viewResolverName = ViewResolver.TEMPLATE_NAME_THYMELEAF;
    private TemplateEngine templateEngine;
}
