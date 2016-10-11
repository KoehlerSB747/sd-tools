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
package org.sd.image;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.sd.util.ThreadPoolUtil;
import org.sd.xml.DataProperties;

/**
 * Class for processing images, controlling for resources allocation.
 * <p>
 * @author Spencer Koehler
 */
public class ImageProcessor {
  
  public static final int DEFAULT_NUM_THREADS = 1;
  public static final boolean USE_THREAD_POOL = false;


  private static final ImageProcessor INSTANCE = new ImageProcessor();
  public static final ImageProcessor getInstance() { return INSTANCE; }


  private ExecutorService threadPool;
  private final Object mutex = new Object();

  private ImageProcessor() {
    this.threadPool = USE_THREAD_POOL ? ThreadPoolUtil.createThreadPool("ImageProcessor-", DEFAULT_NUM_THREADS) : null;
    registerShutdownHook();
  }

  public byte[] resizeImage(byte[] imageBytes, int targetWidth, int targetHeight) {
    byte[] result = null;

    if (this.threadPool != null) {
      final Future<byte[]> future = this.threadPool.submit(new Resizer(imageBytes, targetWidth, targetHeight));
      ThreadPoolUtil.waitUntilDone(new Future<?>[]{future}, 1, 10000);
      if (future.isDone()) {
        try {
          result = future.get();
        }
        catch (InterruptedException ie) {
          System.err.println("ERROR: ImageProcessor.resizeImage(" + imageBytes.length + ") " + ie);
          result = null;
        }
        catch (ExecutionException ee) {
          System.err.println("ERROR: ImageProcessor.resizeImage(" + imageBytes.length + ") " + ee);
          result = null;
        }
        catch (CancellationException ce) {
          System.err.println("ERROR: ImageProcessor.resizeImage(" + imageBytes.length + ") " + ce);
          result = null;
        }
      }
      else {
        System.err.println("WARNING: ImageProcessor.resizeImage(" + imageBytes.length + ") failed to complete resize");
      }
    }
    else {
      try {
        synchronized (mutex) {
          result = ImageUtil.getResizedBytes(imageBytes, targetWidth, targetHeight);
        }
      }
      catch (IOException ioe) {
        System.err.println("ERROR: ImageProcessor.resizeImage(" + imageBytes.length + ") " + ioe);
        result =  null;
      }
    }      

    return result;
  }

  public void close() {
    if (threadPool != null) {
      ThreadPoolUtil.shutdownGracefully(threadPool, 1L);
    }
  }

  private final void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
      new Thread(
        new Runnable() {
          public void run() {
            try {
              close();
            }
            catch (Exception e) {
              //ignore
            }
          }
        },
        "ImageProcessor.ShutdownThread"));
  }


  public static final class Resizer implements Callable<byte[]> {
    public final byte[] imageBytes;
    public final int targetWidth;
    public final int targetHeight;
    private IOException error;

    public Resizer(byte[] imageBytes, int targetWidth, int targetHeight) {
      this.imageBytes = imageBytes;
      this.targetWidth = targetWidth;
      this.targetHeight = targetHeight;
      this.error = null;
    }

    public byte[] call() {
      byte[] result = null;

      try {
        result = ImageUtil.getResizedBytes(imageBytes, targetWidth, targetHeight);
      }
      catch (IOException ioe) {
        this.error = ioe;
      }

      return result;
    }
  }


  public static void main(String[] args) throws Exception {
    // Properties:
    //   inputImage -- the input image file
    //   outputImage -- the output image file
    //   outputType -- the output type (default=jpeg)
    //   either
    //     newWidth -- the new width
    //     newHeight -- the new height
    //   or
    //     newSize -- the new width and height
    //
    //   iters -- number of iterations to run instead of writing the image
    //
    final DataProperties dataProperties = new DataProperties(args);
    args = dataProperties.getRemainingArgs();

    final File inputImage = dataProperties.getFile("inputImage", "workingDir");
    final File outputImage = dataProperties.getFile("outputImage", "workingDir");
    final String outputType = dataProperties.getString("outputType", "jpeg");
    final int newSize = dataProperties.getInt("newSize", -1);
    final int newWidth = dataProperties.getInt("newWidth", newSize);
    final int newHeight = dataProperties.getInt("newHeight", newWidth);
    final int iters = dataProperties.getInt("iters", 1);

    final ImageProcessor imageProcessor = ImageProcessor.getInstance();

    if (newWidth > 0 && newHeight > 0) {
      for (int iter = 0; iter < iters; ++iter) {
        // take the long way around for testing:
        final byte[] imageBytes = ImageUtil.getImageBytes(inputImage);
        final byte[] resizedBytes = imageProcessor.resizeImage(imageBytes, newWidth, newHeight);

        final BufferedImage resizedImage = ImageUtil.createImage(resizedBytes);
        if (iters == 1) {
          ImageUtil.writeImage(resizedImage, outputType, outputImage);
        }
        else {
          if ((iter % 1000) == 0) {
            System.out.println(new Date() + ": resized image " + iter + " times.");
          }
        }
      }
    }

    System.exit(0);
  }
}
