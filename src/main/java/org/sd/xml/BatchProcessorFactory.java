package org.sd.xml;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.sd.xml.XmlStringBuilder;

/**
 * Factory with a shared thread pool for processing a batch of items in parallel.
 * <p>
 * @author Spencer Koehler
 */
public class BatchProcessorFactory {
  
  public static interface ItemProcessor extends Runnable {
    public void setFuture(Future<?> future);
    public Future<?> getFuture();

    /**
     * Add this items' results to the given xml result.
     *
     * @return true if processing was completed; false if future isn't done.
     */
    public boolean addXmlResult(XmlStringBuilder result);
  };


  /** Default max wait time for a batch to finish processing is 5 minutes. */
  public static final int GLOBAL_TIME_LIMIT = 1000 * 60 * 5;

  public final String label;
  private int numThreads;
  private ExecutorService threadPool;

  public BatchProcessorFactory(final String label, int numThreads) {
    this.label = label;
    this.numThreads = numThreads;

    final AtomicInteger threadCount = new AtomicInteger(1);
    this.threadPool =
      Executors.newFixedThreadPool(
        numThreads,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, "BatchProcessor-" + label + "-" + threadCount.getAndIncrement());
          }
        });
  }

  public void close() {
    threadPool.shutdownNow();
  }

  /** Get a new batch processor for processing a batch. */
  public BatchProcessor getBatchProcessor() {
    return new BatchProcessor(threadPool);
  }

  public static final class BatchProcessor {
    private ExecutorService threadPool;
    private List<ItemProcessor> items;
    public final AtomicBoolean die;

    private BatchProcessor(ExecutorService threadPool) {
      this.threadPool = threadPool;
      this.items = new ArrayList<ItemProcessor>();
      this.die = new AtomicBoolean(false);
    }

    /** Submit an item for processing. */
    public void submit(ItemProcessor itemProcessor) {
      if (itemProcessor != null) {
        final Future<?> future = threadPool.submit(itemProcessor);
        itemProcessor.setFuture(future);
        this.items.add(itemProcessor);
      }
    }

    /** Get the number of items in this batch. */
    public int getBatchSize() {
      return items.size();
    }

    /**
     * Wait at most timeLimitMillis for results.
     * <p>
     * If timeLimitMillis == 0, wait indefinitely (until liberal global timelimit) until finished.
     * If timeLimitMillis &lt; 0, return the items immediately.
     *
     * @return all items, whether finished or not.
     */
    public List<ItemProcessor> waitForResults(long timeLimitMillis) {
      if (timeLimitMillis < 0) return items;
      else if (timeLimitMillis == 0) timeLimitMillis = GLOBAL_TIME_LIMIT;

      final long starttime = System.currentTimeMillis();

      for (int runningCount = countRunningItems(); runningCount > 0;
           runningCount = countRunningItems()) {
        if (timeLimitMillis > 0 && (System.currentTimeMillis() - starttime) >= timeLimitMillis) {
          break;
        }
      }

      return items;
    }

    /** Count the number of running items in this batch. */
    public final int countRunningItems() {
      int result = 0;

      for (ItemProcessor item : items) {
        final Future<?> future = item.getFuture();
        if (future != null && !future.isDone()) {
          ++result;
        }
      }

      return result;
    }
  }
}
