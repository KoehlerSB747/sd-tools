/*
   Copyright 2008-2016 Semantic Discovery, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.sd.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities for working with thread pools.
 * <p>
 * @author Spencer Koehler
 */
public class ThreadPoolUtil {
  
  public interface CancelFunction {
    public void prepareForCancel(int futureIdx, Future<?> future);
  }


  /**
   * Create and initialize a fixed size thread pool.
   *
   * @param poolNamePrefix  the prefix for thread names (e.g. "myPool-" for threads named "myPool-N")
   * @param numThreads  the number of threads for the pool.
   * 
   * @return the thread pool
   */
  public static final ExecutorService createThreadPool(final String poolNamePrefix, final int numThreads) {
    final AtomicInteger threadCount = new AtomicInteger(1);
    final ExecutorService result =
      Executors.newFixedThreadPool(
        numThreads,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, poolNamePrefix + threadCount.getAndIncrement());
          }
        });
    return result;
  }

  /**
   * Shutdown the pool, waiting for tasks to terminate.
   *
   * @param pool  The thread pool to shut down
   * @param maxSecondsToWait  The time in seconds to wait before cancelling tasks
   *
   * @return true if all tasks successfully completed; otherwise false
   */
  public static final boolean shutdownGracefully(ExecutorService pool, long maxSecondsToWait) {
    boolean result = true;

    pool.shutdown();  // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(maxSecondsToWait, TimeUnit.SECONDS)) {
        pool.shutdownNow();  // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(maxSecondsToWait, TimeUnit.SECONDS)) {
          result = false;
        }
      }
    }
    catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
      result = false;
    }

    return result;
  }

  /**
   * Count the number of futures that are done.
   *
   * @param futures  The futures to check
   *
   * @return the number of non-null futures that are done.
   */
  public static final int countCompleted(List<Future<?>> futures) {
    int result = 0;

    if (futures != null) {
      for (Future<?> future : futures) {
        if (future != null && future.isDone()) {
          ++result;
        }
      }
    }

    return result;
  }

  /**
   * Wait until the futures are all done
   *
   * @param futures  The futures to wait for (ok if some are null)
   * @param pollingMillis  The amount of time to wait between polling the futures
   * @param timeoutMillis  Overall timeout (unlimited if 0)
   *
   * @return true if all finished normally/successfully;
   *         otherwise, false (e.g., if interrupted)
   */
  public static final boolean waitUntilDone(Future<?>[] futures, long pollingMillis, long timeoutMillis) {
    // return true if finished normally/successfully
    boolean result = true;

    final long starttime = System.currentTimeMillis();

    // add those that are not yet done to a list
    final List<Future<?>> notdone = new ArrayList<Future<?>>();
    for (Future<?> future : futures) {
      if (future != null && !future.isDone()) {
        notdone.add(future);
      }
    }

    // drain the list as futures finish
    while (notdone.size() > 0) {
      // give things some time
      try {
        Thread.sleep(pollingMillis);
      }
      catch (InterruptedException ie) {
        result = false;  // interrupted
        // Preserve interrupt status
        Thread.currentThread().interrupt();
        break;
      }

      // check each
      if (!removeFinished(notdone)) {
        result = false;
      }

      if (timeoutMillis > 0 && (System.currentTimeMillis() - starttime) > timeoutMillis) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Limited wait for each future to finish.
   *
   * @param futures  The futures to wait for (ok if some are null)
   * @param timeoutMillis  Maximum time to wait for any future (unlimited if 0)
   * @param cancelFunction  CancelFunction to call when time limit is reached before calling future.cancel.
   * @param cancel  True to cancel each future as it reaches its time limit.
   *
   * @return true if all finished normally/successfully;
   *         otherwise, false (e.g., if interrupted)
   */
  public static final boolean waitForEach(Future<?>[] futures, long timeoutMillis, CancelFunction cancelFunction, boolean cancel) {
    // return true if finished normally/successfully
    boolean result = true;

    for (int futureIdx = 0; futureIdx < futures.length; ++futureIdx) {
      final Future<?> future = futures[futureIdx];

      if (future != null && !future.isDone()) {
        try {
          if (timeoutMillis <= 0L) {
            future.get();
          }
          else {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
          }
        }
        catch (InterruptedException ie) {
          // Preserve interrupt status
          Thread.currentThread().interrupt();
        }
        catch (Exception e) {
          // done waiting for get
        }
 
        if (!future.isDone()) {
          result = false;
          if (cancelFunction != null) {
            cancelFunction.prepareForCancel(futureIdx, future);
          }
          if (cancel) {
            future.cancel(true);
          }
        }
      }
    }

    return result;
  }

  /**
   * Remove finished tasks from the list of futures.
   *
   * @param futures  The list of futures to remove finished tasks from.
   *
   * @return true if all removed finished normally/successfully;
   *         otherwise, false (e.g., if cancelled)
   */
  public static final boolean removeFinished(List<Future<?>> futures) {
    boolean result = true;

    for (Iterator<Future<?>> iter = futures.iterator(); iter.hasNext(); ) {
      final Future<?> future = iter.next();
      if (future.isDone()) {
        iter.remove();

        if (future.isCancelled()) {
          result = false;
        }
      }
    }

    return result;
  }
}
