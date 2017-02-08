/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.io.http.impl;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.schedulers.Schedulers;

/**
 * A thread pool executor that is used to do perform all callbacks to the subscribers of Observables returned by
 * {@link CaravanHttpClientImpl#execute(io.wcm.caravan.io.http.request.CaravanHttpRequest)}. This is desired to avoid
 * the threads that actually execute the HTTP request are being blocked by client code. This thread pool has a core size
 * of just four threads, but if all of these threads are used by slow callback code, additional threads are
 * automatically spawned on demand.
 */
public class CaravanHttpCallbackExecutor extends ThreadPoolExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(CaravanHttpCallbackExecutor.class);

  static final String THREAD_GROUP_NAME = "Caravan-Http-Callbacks";

  private static final AtomicInteger THREAD_INDEX_COUNTER = new AtomicInteger();

  private static final int THREAD_POOL_CORE_SIZE = 4;
  private static final int THREAD_POOL_MAX_SIZE = 1000;
  private static final int IDLE_THREAD_KEEP_ALIVE_SECONDS = 10;

  private static final int WATCHDOG_INTERVAL_SECONDS = 1;

  private final rx.Scheduler.Worker watchdogWorker;

  CaravanHttpCallbackExecutor() {
    // by using a LinkedBlockingQueue, the ThreadPoolExecutor will not automatically spawn any new threads when all core threads are in use,
    // instead, additional tasks will be queued up. If this happens, the periodically called ::increaseCoreSizeIfJobsAreQeueued method will adjust the core size
    super(THREAD_POOL_CORE_SIZE, THREAD_POOL_MAX_SIZE, IDLE_THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
        new RejectionHandler());


    watchdogWorker = Schedulers.computation().createWorker();
    watchdogWorker.schedulePeriodically(this::increaseCoreSizeIfJobsAreQeueued, 5, WATCHDOG_INTERVAL_SECONDS, TimeUnit.SECONDS);

    setThreadFactory(runnable -> new Thread(runnable, THREAD_GROUP_NAME + "-" + THREAD_INDEX_COUNTER.getAndIncrement()));
  }

  @Override
  public void shutdown() {

    watchdogWorker.unsubscribe();

    super.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {

    watchdogWorker.unsubscribe();

    return super.shutdownNow();
  }

  private void increaseCoreSizeIfJobsAreQeueued() {

    // This is based on "Idea #2" described in the following comment
    // http://stackoverflow.com/questions/9622599/java-threadpoolexecutor-strategy-direct-handoff-with-queue#answer-9623100

    // the idea is that we don't allow ThreadPoolExecutor to decide when to spawn new thread on its own, as this could lead to a huge threadpool if many
    // http requests are initiated at once. As long as the callbacks are executed fast, the system should work well by just using the core threadpool

    int queueSize = getQueue().size();
    int currentPoolSize = getPoolSize();
    int corePoolSize = getCorePoolSize();
    int activeCount = getActiveCount();
    try {

      if (LOG.isTraceEnabled()) {
        LOG.trace(
            "Checking if threadool core size should be adjusted: queueSize=" + queueSize + ". poolSize=" + currentPoolSize + ", activeCount=" + activeCount);
      }

      if (queueSize > 0 && activeCount == currentPoolSize && currentPoolSize < getMaximumPoolSize()) {
        int newCoreSize = Math.min(currentPoolSize + queueSize, getMaximumPoolSize());
        LOG.warn(
            "Increasing thread pool core size to " + newCoreSize + ", because there are " + queueSize + " callbacks waiting in the queue, and all of "
                + currentPoolSize + " threads are blocked.");

        setCorePoolSize(newCoreSize);
      }
      else if (queueSize == 0 && activeCount < currentPoolSize && corePoolSize > THREAD_POOL_CORE_SIZE) {
        int newCoreSize = corePoolSize - 1;

        LOG.info(
            "Decreasing thread pool core size to " + newCoreSize + ", because there are no callbacks waiting in the queue, and only " + activeCount + " of "
                + currentPoolSize + " threads are blocked.");

        setCorePoolSize(newCoreSize);
      }
    }
    catch (IllegalArgumentException ex) {
      LOG.error("Failed to adjust core size queueSize=" + queueSize + ". poolSize=" + currentPoolSize + ", activeCount=" + activeCount, ex);
    }

  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {

    if (LOG.isTraceEnabled()) {
      LOG.trace("Executing HTTP callback on thread " + t + ", current pool size is " + getPoolSize());
    }

    super.beforeExecute(t, r);
  }


  private static class RejectionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      LOG.error("Rejected to execute async http callback " + r.toString() + " because the maximum number of threads (" + executor.getMaximumPoolSize()
      + ") has been reached");
    }

  }

}
