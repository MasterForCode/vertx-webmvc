package top.soliloquize.vertxmvc.core;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.function.Supplier;

/**
 * @author wb
 * @date 2020/1/16
 */
public class VertxUtils {
    public static <T> Promise<T> executeBlocking(Vertx vertx, Supplier<T> supplier) {
        Promise<T> result = Promise.promise();
        vertx.executeBlocking(promise -> VertxUtils.completeFuture(promise, supplier), false, result.future());
        return result;
    }

    public static <T> void completeFuture(Promise<T> promise, Supplier<T> supplier) {
        T obj;
        try {
            obj = supplier.get();
        } catch (Throwable var4) {
            promise.fail(var4);
            return;
        }
        promise.complete(obj);
    }

//
    public static <T> Future<T> executeBlockingEx(Vertx vertx, Supplier<T> supplier) {
        Future<T> resultFuture = Future.future();
        vertx.executeBlocking((future) -> {
            VertxUtils.completeFuture(future, supplier);
        }, false, resultFuture.completer());
        return resultFuture;
    }

    public static <T> void completeFuture(Future<T> future, Supplier<T> supplier) {
        T obj = null;

        try {
            obj = supplier.get();
        } catch (Throwable var4) {
            future.fail(var4);
            return;
        }

        future.complete(obj);
    }
}
