package top.soliloquize.vertxmvc.controller;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.redis.client.Response;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.soliloquize.vertxmvc.Singleton;
import top.soliloquize.vertxmvc.annotations.Blocking;
import top.soliloquize.vertxmvc.core.SyncWrapper;

/**
 * @author wb
 * @date 2020/6/15
 */
@RestController
@RequestMapping(value = "redis")
public class RedisController {
    @GetMapping(value = "get/:key")
    @Blocking
    public String get(String key) throws InterruptedException {
        Future<Response> fut1 = Future.future(promise -> {

            Singleton.redis.get(key, promise);
        });
        AsyncResult<Response> result = new SyncWrapper<Response>().waitForFutureToComplete(fut1, 3);

        if (result.succeeded()) {
            return result.result().toString();
        } else {
            throw new RuntimeException(result.cause());
        }
    }
}
