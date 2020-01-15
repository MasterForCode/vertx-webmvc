package top.soliloquize.vertxmvc.controller;

import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.soliloquize.vertxmvc.entity.User;
import top.soliloquize.vertxmvc.service.UserService;

import java.util.List;

/**
 * @author wb
 * @date 2019/9/27
 */
@Controller
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/all")
    @GetMapping("/dad")
    public List<User> test(List<Integer> a) {
        return userService.getAllUsers();
    }

    @RequestMapping(value = "/one/:id")
    public User test(Long id) {
        return userService.getById(id);
    }

    @PostMapping(value = "/one")
    public User add(@RequestBody User user) {
        return userService.addOne(user);
    }

    @PostMapping(value = "/upload")
    public void upload(RoutingContext rc) {
        System.out.println("--------");
        rc.fileUploads().forEach(each -> System.out.println(each.fileName()));
    }
}
