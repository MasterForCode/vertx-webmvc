package top.soliloquize.vertxmvc.exceptions;

/**
 * 当控制器请求路径异常时使用该异常
 * @author wb
 * @date 2019/9/27
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }
}
