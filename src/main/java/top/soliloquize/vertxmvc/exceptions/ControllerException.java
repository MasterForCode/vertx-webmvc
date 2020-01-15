package top.soliloquize.vertxmvc.exceptions;

/**
 * @author wb
 * @date 2019/9/28
 */
public class ControllerException extends RuntimeException {

    public ControllerException(String message) {
        super(message);
    }
}
