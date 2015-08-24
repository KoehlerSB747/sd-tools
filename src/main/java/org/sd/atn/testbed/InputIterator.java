package org.sd.atn.testbed;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sd.token.NamedEntitySegmentFinder;
import org.sd.token.SegmentPointer;
import org.sd.token.SegmentPointerFinder;
import org.sd.token.SegmentPointerIterator;
import org.sd.token.SegmentPointerFinderFactory;
import org.sd.token.WordCharacteristics;
import org.sd.token.WordFinder;
import org.sd.util.SentenceIterator;
import org.sd.xml.DataProperties;

/**
 * Iterator over input segments to pass to extraction.
 * <p>
 * @author Spence Koehler
 */
public class InputIterator implements Iterator<String> {
  
  public static final String ONE_LINE_KEY = "oneLine";
  public static final String NAMED_ENTITY_SPLIT_KEY = "namedEntitySplit";
  public static final String NAMED_ENTITIES_ONLY_KEY = "namedEntitiesOnly";
  public static final String VERTICAL_BAR_SPLIT_KEY = "verticalBarSplit";
  public static final String IGNORE_CAPS_CHANGE_KEY = "ignoreCapsChange";
  public static final String MAX_INPUT_LENGTH_KEY = "maxInputLength";


  private String input;
  private DataProperties options;
  private DataProperties overrides;

  private String[] strings;
  private int nextStringIndex;

  public InputIterator(String input, DataProperties options) {
    this(input, options, null);
  }

  public InputIterator(String input, DataProperties options, DataProperties overrides) {
    this.input = input;
    this.options = options;
    this.overrides = overrides;

    this.strings = null;
    this.nextStringIndex = 0;
  }

  public boolean hasNext() {
    if (strings == null) strings = buildStrings(input, options, overrides);
    return nextStringIndex < strings.length;
  }

  public String next() {
    if (strings == null) strings = buildStrings(input, options, overrides);
    return strings[nextStringIndex++];
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }


  private final String[] buildStrings(String input, DataProperties options, DataProperties overrides) {
    final String[] result = doBuildStrings(input, options, overrides);
    return (result == null) ? new String[]{} : result;
  }

  public static final boolean getBoolean(DataProperties options, DataProperties overrides, String propertyName, boolean defaultValue) {
    boolean result = defaultValue;

    if (overrides != null) {
      result = overrides.getBoolean(propertyName, defaultValue);
    }
    else if (options != null) {
      result = options.getBoolean(propertyName, defaultValue);
    }

    return result;
  }

  public static final int getInt(DataProperties options, DataProperties overrides, String propertyName, int defaultValue) {
    int result = defaultValue;

    if (overrides != null) {
      result = overrides.getInt(propertyName, defaultValue);
    }
    else if (options != null) {
      result = options.getInt(propertyName, defaultValue);
    }

    return result;
  }

  protected String[] doBuildStrings(String input, DataProperties options, DataProperties overrides) {
    final List<String> result = new ArrayList<String>();

    final boolean oneLine = getBoolean(options, overrides, ONE_LINE_KEY, false);
    final int maxInputLength = getInt(options, overrides, MAX_INPUT_LENGTH_KEY, 0);

    if (maxInputLength > 0 && input.length() > maxInputLength) {
      input = input.substring(0, maxInputLength);
    }

    if (oneLine) {
      result.add(input);
    }
    else {
      final boolean namedEntitySplit = getBoolean(options, overrides, NAMED_ENTITY_SPLIT_KEY, false);
      final boolean namedEntitiesOnly = getBoolean(options, overrides, NAMED_ENTITIES_ONLY_KEY, false);
      final boolean verticalBarSplit = getBoolean(options, overrides, VERTICAL_BAR_SPLIT_KEY, false);
      final boolean ignoreCapsChange = getBoolean(options, overrides, IGNORE_CAPS_CHANGE_KEY, false);

      final SegmentPointerFinderFactory ptrFactory = 
        namedEntitySplit ? NamedEntitySegmentFinder.getFactory(ignoreCapsChange) : null;

      String[] inputPieces = null;

      if (verticalBarSplit) {
        // replace vertical bars representing new input lines with new lines
        inputPieces = fixVerticalBars(input);
      }
      else {
        inputPieces = new String[]{input};
      }

      int seqNum = 0;
      for (String inputPiece : inputPieces) {
        for (SentenceIterator sentenceIter = new SentenceIterator(inputPiece).setDetectAbbrev(true).setGreedy(false); sentenceIter.hasNext(); ) {
          final String sentence = sentenceIter.next();

          if (namedEntitySplit) {
            final SegmentPointerFinder ptrFinder = 
              ptrFactory.getSegmentPointerFinder(sentence);
            for (SegmentPointerIterator ptrIter = new SegmentPointerIterator(ptrFinder); ptrIter.hasNext(); ) {
              final SegmentPointer ptr = ptrIter.next();
              ptr.setSeqNum(seqNum++);

              if (namedEntitiesOnly) {
                if (!NamedEntitySegmentFinder.ENTITY_LABEL.equals(ptr.getLabel())) {
                  continue;
                }
              }

              if (ptr.hasInnerSegments()) {
                for (SegmentPointer.InnerSegment innerSegment : ptr.getInnerSegments()) {
                  result.add(ptr.getText(innerSegment));
                }
              }
              else {
                result.add(ptr.getText());
              }
            }
          }
          else {
            result.add(sentence);
          }
        }
      }
    }

    return result.size() == 0 ? null : result.toArray(new String[result.size()]);
  }

  protected String[] fixVerticalBars(String input) {
    int vbPos = input.indexOf('|');
    if (vbPos < 0) return new String[]{input};

    final List<String> result = new ArrayList<String>();

    final StringBuilder builder = new StringBuilder();
    final String[] pieces = input.split("\\s*\\|+\\s*");
    for (String piece : pieces) {
      if (isOwnLine(piece)) {
        if (builder.length() > 0) {
          result.add(builder.toString());
          builder.setLength(0);
        }
        result.add(piece);
      }
      else {
        if (builder.length() > 0) builder.append(' ');
        builder.append(piece);
      }
    }

    if (builder.length() > 0) {
      result.add(builder.toString());
    }

    return result.toArray(new String[result.size()]);
  }

  protected boolean isOwnLine(String input) {
    boolean result = true;

    int seqNum = 0;
    for (SegmentPointerIterator ptrIter = new SegmentPointerIterator(new WordFinder(input)); ptrIter.hasNext(); ) {
      final SegmentPointer ptr = ptrIter.next();
      ptr.setSeqNum(seqNum++);

      final WordCharacteristics wc = ptr.getWordCharacteristics();
      if (!wc.firstIsUpper(true)) {
        result = false;
        break;
      }

      if (seqNum > 6) {
        result = false;
        break;
      }
    }

    return result;
  }


  private final String getSubstring(String input, int startPos, int endPos, boolean hasVB) {
    String substring = input.substring(0, startPos).trim();
    if (hasVB) {
      substring = substring.replaceAll("\\|", " ");
    }
    return substring;
  }


  public static void main(String[] args) {
    // Properties:
    //   oneLine -- true/false
    //   namedEntitySplit -- true/false
    //   namedEntitiesOnly -- true/false
    //   verticalBarSplit -- true/false

    final DataProperties options = new DataProperties(args);
    args = options.getRemainingArgs();

    for (String arg : args) {
      System.out.println("Input: '" + arg + "'");
      for (InputIterator iter = new InputIterator(arg, options); iter.hasNext(); ) {
        final String line = iter.next();
        System.out.println("\t" + line);
      }
    }
  }
}
