/*
   Copyright 2008-2015 Semantic Discovery, Inc.

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
package org.sd.atn.testbed;


import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.sd.atn.XmlInputDecoder;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Adapter for XML or string input into LineExtractor for submitting to
 * AtnParseRunner.
 * <p>
 * @author Spence Koehler
 */
public class InputAdapter implements Iterator<AtnInput> {

  private XmlInputDecoder decoder;

  private int nextID;
  private DataProperties options;
  private DataProperties overrides;

  private Stack<XmlInputDecoder.Paragraph> paragraphs;

  private InputAdapter(DataProperties options, DataProperties overrides) {
    this.nextID = 0;
    this.options = options;
    this.overrides = (overrides != null) ? overrides : new DataProperties();
  }

  /** Construct with the given input string. */
  public InputAdapter(String input, DataProperties options, DataProperties overrides) {
    this(options, overrides);
    this.decoder = new XmlInputDecoder(input, InputIterator.getBoolean(options, overrides, InputIterator.ONE_LINE_KEY, false));
    init();
  }

  /**
   * Construct with the given xml text input.
   * <p>
   * See XmlInputDecoder for expected xml format.
   */
  public InputAdapter(DomElement textElement, DataProperties options, DataProperties overrides) {
    this(options, overrides);
    this.decoder = new XmlInputDecoder(textElement);
    init();
  }

  public XmlInputDecoder getDecoder() {
    return decoder;
  }

  public DataProperties getOptions() {
    return options;
  }

  public DataProperties getOverrides() {
    return overrides;
  }


  private final void init() {
    this.paragraphs = new Stack<XmlInputDecoder.Paragraph>();
    final List<XmlInputDecoder.Paragraph> decodedParagraphs = decoder.getParagraphs();
    for (int i = decodedParagraphs.size() - 1; i >= 0; --i) {
      this.paragraphs.push(decodedParagraphs.get(i));
    }
  }

  public boolean hasNext() {
    return !paragraphs.empty();
  }

  public AtnInput next() {
    XmlInputDecoder.Paragraph paragraph = paragraphs.pop();

    // split if indicated and warranted
    if (!paragraph.oneLine()) {
      final InputIterator iter = new InputIterator(paragraph.getText(), options, overrides);
      final List<XmlInputDecoder.Paragraph> splits = paragraph.split(iter);
      for (int i = splits.size() - 1; i > 0; --i) {
        paragraphs.push(splits.get(i));
      }
      paragraph = splits.get(0);
    }

    return new AtnInput(paragraph, nextID++, options, overrides);
  }

  public void remove() {
    throw new UnsupportedOperationException("Implement if/when needed.");
  }
}
