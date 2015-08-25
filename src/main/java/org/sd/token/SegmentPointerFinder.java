/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.token;


/**
 * Thread-safe interface for finding segment pointer boundaries.
 * <p>
 * @author Spence Koehler
 */
public interface SegmentPointerFinder {
  
  /** Get the input */
  public String getInput();

  /** Get the input length */
  public int length();

  /**
   * Find the start pointer position at or beyond fromPos (e.g., skip over
   * whitespace, etc.)
   */
  public int findStartPtr(int fromPos);

  /**
   * Find the segment beginning at the startPtr.
   *
   * @return the segment or null if there is no segment to be found.
   */
  public SegmentPointer findSegmentPointer(int startPtr);

  /** Get the normalizer, or null. */
  public Normalizer getNormalizer();

  /** Set the normalizer (okay if null). */
  public void setNormalizer(Normalizer normalizer);

}
