package top.soliloquize.vertxmvc.exceptions;

/**
 * 当部署vertx异常时使用该异常
 * @author wb
 * @date 2020/1/16
 */
public class DeployException extends Exception {
    public DeployException(String message, Throwable cause) {
        super(message,cause);
    }
}
