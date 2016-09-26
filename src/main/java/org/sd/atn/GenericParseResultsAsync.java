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
package org.sd.atn;


import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.util.ThreadPoolUtil;
import org.sd.xml.DataProperties;

/**
 * Wrapper for generating parse results asynchronously.
 * <p>
 * @author Spencer Koehler
 */
public class GenericParseResultsAsync {
  
  public final ExecutorService threadPool;
  public final GenericParser genericParser;
  public final String inputText;
  public final DataProperties options;
  public final AtomicBoolean die;
  private Future<GenericParseResults> future;
  private boolean ownThreadPool;

  GenericParseResultsAsync(final ExecutorService threadPool, final GenericParser genericParser,
                           final String inputText, final DataProperties options) {
    this.threadPool = getThreadPool(threadPool);
    this.genericParser = genericParser;
    this.inputText = inputText;
    this.options = options;
    this.die = new AtomicBoolean(false);
    this.future = this.threadPool.submit(new Callable<GenericParseResults>() {
        public GenericParseResults call() {
          return genericParser.parse(inputText, options, die);
        }
      });
  }

  private final ExecutorService getThreadPool(ExecutorService threadPool) {
    ExecutorService result = threadPool;
    this.ownThreadPool = false;

    if (result == null) {
      result = ThreadPoolUtil.createThreadPool("GenericParseResultAsync-", 1);
      this.ownThreadPool = true;
    }

    return result;
  }

  public void close() {
    if (!completed() && !stopped()) {
      if (stopParsing()) stopParsing();
    }
    if (ownThreadPool) {
      ThreadPoolUtil.shutdownGracefully(threadPool, 1L);
    }
  }

  public Future<?> getFuture() {
    return future;
  }

  public boolean isDone() {
    return future.isDone();
  }

  public boolean completed() {
    return future.isDone() && !future.isCancelled() && !die.get();
  }

  public boolean stopped() {
    return die.get() || future.isCancelled();
  }

  public boolean stopParsing() {
    boolean result = false;

    if (!die.compareAndSet(false, true)) {
      die.set(true);
      result = true;
    }
    else {
      future.cancel(true);
    }

    return result;
  }

  /**
   * Get the parse results, or null if not done.
   */
  public GenericParseResults getParseResults() {
    GenericParseResults result = null;

    if (future.isDone()) {
      try {
        result = future.get();
      }
      catch (InterruptedException ie) {
        result = null;
      }
      catch (ExecutionException ee) {
        throw new IllegalStateException(ee);
      }
      catch (CancellationException ce) {
        result = null;
      }
    }

    return result;
  }

  /**
   * Wait for the parse results up to timeoutMillis before stopping parsing,
   * waiting an additional waitToDieMillis and retrieving the results.
   */
  public GenericParseResults getParseResults(final long waitToDieMillis, final long timeoutMillis) {
    ThreadPoolUtil.waitForEach(new Future<?>[]{future}, timeoutMillis, new ThreadPoolUtil.CancelFunction() {
        public void prepareForCancel(int futureIdx, Future<?> future) {
          stopParsing();

          if (waitToDieMillis > 0 && !future.isDone()) {
            // give some time for "die" to take effect
            try {
              Thread.sleep(waitToDieMillis);
            }
            catch (InterruptedException ie) {
              // Preserve interrupt status
              Thread.currentThread().interrupt();
            }
          }
        }
      }, true);

    return getParseResults();
  }
}
