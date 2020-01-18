package top.soliloquize.vertxmvc.exceptions;

/**
 * 当参数异常时使用该异常
 * @author wb
 * @date 2019/9/28
 */
public class ParameterExeception extends RuntimeException {
    public ParameterExeception(String message) {
        super(message);
    }
}
