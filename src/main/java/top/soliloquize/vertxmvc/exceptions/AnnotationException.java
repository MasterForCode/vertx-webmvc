package top.soliloquize.vertxmvc.exceptions;

/**
 * 当注解错误时使用该异常
 * @author wb
 * @date 2019/9/28
 */
public class AnnotationException extends RuntimeException {
    public AnnotationException(String message, Throwable cause) {
        super(message,cause);
    }
}
