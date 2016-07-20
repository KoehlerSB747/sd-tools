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


import java.io.IOException;
import java.io.Writer;

/**
 * Interface for writing dot data.
 * <p>
 * @author Spencer Koehler
 */
public interface DotWriter {
  
  /**
   * Set a graph, node, or edge attribute.
   * <ul>
   * <li>if key.startsWith("node:"), then set a node attribute e.g. "node:fontsize=9"</li>
   * <li>else if key.startsWith("edge:"), then set an edge attribute e.g., "edge:fontsize=9"</li>
   * <li>else, set a graph attribute e.g. "fontsize=9"</li>
   * </ul>
   *
   * @param key  The key
   * @param value  The value
   */
  public void setAttribute(String key, String value);

  /**
   * Write the dot output to the writer.
   *
   * @param writer  the writer to write to
   *
   * @throws IOException
   */
  public void writeDot(Writer writer) throws IOException;

}
