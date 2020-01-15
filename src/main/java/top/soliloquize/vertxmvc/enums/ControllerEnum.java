package top.soliloquize.vertxmvc.enums;

/**
 * @author wb
 * @date 2019/9/27
 */
public enum ControllerEnum {
    /**
     * controller
     */
    CONTROLLER("controller"),
    /**
     * restController
     */
    REST_CONTROLLER("restController");
    private String name;

    ControllerEnum(String name) {
        this.name = name;
    }
}
