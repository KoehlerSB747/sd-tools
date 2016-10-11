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


import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import org.sd.xml.DataProperties;

/**
 * Utilities for operations on images.
 * <p>
 * @author Spencer Koehler
 */
public class ImageUtil {
  
  public static final byte[] getImageBytes(File imageFile) throws IOException {
    byte[] result = null;

    if (imageFile != null && imageFile.exists()) {
      final BufferedImage bufferedImage = readImage(imageFile);
      result = getImageBytes(bufferedImage);
    }

    return result;
  }

  public static final byte[] getImageBytes(BufferedImage bufferedImage) throws IOException {
    byte[] result = null;

    if (bufferedImage != null) {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        ImageIO.write(bufferedImage, "jpeg", baos);
        result = baos.toByteArray();
      }
      finally {
        baos.close();
      }
    }

    return result;
  }

  public static BufferedImage createImage(byte[] imageBytes) throws IOException {
    BufferedImage result = null;

    if (imageBytes != null && imageBytes.length > 0) {
      final ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
      try {
        result = readImage(inputStream);
      }
      finally {
        inputStream.close();
      }
    }

    return result;
  }

  public static BufferedImage readImage(Object fileOrInputStream) throws IOException {
    BufferedImage result = null;

    final ImageInputStream stream = ImageIO.createImageInputStream(fileOrInputStream);
    try {
      final Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
      while (iter.hasNext()) {
        final ImageReader reader = iter.next();
        reader.setInput(stream);

        try {
          result = reader.read(0);
          if (result != null) {
            break;
          }
        }
        finally {
          reader.dispose();
        }
      }
    }
    finally {
      if (stream != null) {
        stream.close();
      }
    }

    return result;
  }

  public static final void writeImage(BufferedImage bufferedImage, String formatName, File outputFile) throws IOException {
    ImageIO.write(bufferedImage, formatName, outputFile);
  }

  public static final BufferedImage resizeImage(BufferedImage img, int newWidth, int newHeight) {
    BufferedImage result = null;

    if (img != null) {
      result = new BufferedImage(newWidth, newHeight, img.getType());
      final Graphics2D g = result.createGraphics();
      try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newWidth, newHeight, 0, 0, img.getWidth(), img.getHeight(), null);
      }
      finally {
        g.dispose();
      }
    }

    return result;
  }

  public static final byte[] getResizedBytes(byte[] imageBytes, int newWidth, int newHeight) throws IOException {
    final BufferedImage image = createImage(imageBytes);
    final BufferedImage result = resizeImage(image, newWidth, newHeight);
    return getImageBytes(result);
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

    if (newWidth > 0 && newHeight > 0) {
      for (int iter = 0; iter < iters; ++iter) {
        // take the long way around for testing:
        final byte[] imageBytes = getImageBytes(inputImage);
        final byte[] resizedBytes = getResizedBytes(imageBytes, newWidth, newHeight);

        final BufferedImage resizedImage = createImage(resizedBytes);
        if (iters == 1) {
          writeImage(resizedImage, outputType, outputImage);
        }
        else {
          if (iter > 0 && (iter % 1000) == 0) {
            System.out.println(new Date() + ": resized image " + iter + " times.");
          }
        }
      }
    }
  }
}
