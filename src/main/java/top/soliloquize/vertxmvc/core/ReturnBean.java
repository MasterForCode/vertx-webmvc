package top.soliloquize.vertxmvc.core;

import lombok.Builder;
import lombok.Data;

/**
 * @author wb
 * @date 2020/1/15
 */
@Data
@Builder
public class ReturnBean {
    @Builder.Default
    private int code = 200;
    private String msg;
    private Object data;
}
