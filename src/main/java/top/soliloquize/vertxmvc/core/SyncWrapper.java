package top.soliloquize.vertxmvc.core;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author wb
 * @date 2020/6/16
 */
public class SyncWrapper<T> {

    private CountDownLatch latch = new CountDownLatch(1);

    private AsyncResult<T> ar;

    //private int timeoutSeconds = 0;

    public AsyncResult waitForFutureToComplete(Future<T> future) {
        return waitForFutureToComplete(future, 0);
    }

    public AsyncResult<T> waitForFutureToComplete(Future<T> future, int timeoutSeconds) {
        if (future == null) {
            return Future.failedFuture("Future is null");
        }

        future.onComplete(ar1 -> {
            this.ar = ar1;
            latch.countDown();
        });

        try {
            if (timeoutSeconds == 0) {
                latch.await();
            } else {
                if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                    ar = Future.failedFuture("Wait for async operation to complete timeout: " + timeoutSeconds + "s");
                }
            }
        } catch (InterruptedException e) {
            ar = Future.failedFuture(e);
        }

        return ar;
    }

}
