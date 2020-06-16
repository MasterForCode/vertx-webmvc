package top.soliloquize.vertxmvc.exceptions;

/**
 * 当控制器异常时使用该异常
 * @author wb
 * @date 2019/9/28
 */
public class ControllerException extends RuntimeException {

    public ControllerException(String message, Throwable cause) {
        super(message,cause);
    }
}
