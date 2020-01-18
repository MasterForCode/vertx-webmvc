package top.soliloquize.vertxmvc.exceptions;

/**
 * 当返回值异常时使用该异常
 * @author wb
 * @date 2020/1/18
 */
public class ReturnDataException extends RuntimeException {
    public ReturnDataException(String message) {
        super(message);
    }
}
