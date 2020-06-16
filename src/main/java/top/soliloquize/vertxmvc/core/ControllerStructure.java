package top.soliloquize.vertxmvc.core;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * controller详情
 * @author wb
 * @date 2020/1/16
 */
@Data
@Builder
public class ControllerStructure {
    private String path;
    private String controllerName;
    private Object controller;
    @Builder.Default
    List<MethodStructure> methodStructureList = new ArrayList<>();
}
