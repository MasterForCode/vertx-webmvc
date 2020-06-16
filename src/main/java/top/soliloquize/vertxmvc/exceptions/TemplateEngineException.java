package top.soliloquize.vertxmvc.exceptions;

/**
 * 当模板引擎异常时使用该异常
 * @author wb
 * @date 2020/1/18
 */
public class TemplateEngineException extends RuntimeException {
    public TemplateEngineException(String message, Throwable cause) {
        super(message,cause);
    }
}
