package top.soliloquize.vertxmvc;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisConnection;
import top.soliloquize.vertxmvc.core.Initialization;
import top.soliloquize.vertxmvc.spring.Springs;

/**
 * @author wb
 * @date 2020/6/4
 */
public class MainVerticle extends DefaultVerticle {
    public static void main(String[] args) {
        new MainVerticle().run(null, new Initialization() {
            @Override
            public void initSpring(Vertx vertx) {
                Springs.initByFile("classpath:applicationContext.xml");
                Redis.createClient(vertx, "redis://192.168.88.125:6379")
                        .connect(onConnect -> {
                            if (onConnect.succeeded()) {
                                RedisConnection client = onConnect.result();
                                Singleton.redis = RedisAPI.api(client);
                            }
                        });
            }
        });
    }
}
