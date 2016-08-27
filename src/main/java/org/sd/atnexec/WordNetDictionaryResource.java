package org.sd.atnexec;


import java.io.File;
import java.io.IOException;
import org.sd.atn.ResourceManager;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.xml.DomElement;

/**
 * Wrapper around a WordNet LexDictionary to use as an ATN parser resource.
 * <p>
 * @author Spencer Koehler
 */
public class WordNetDictionaryResource extends LexDictionary {
  
  public WordNetDictionaryResource(DomElement resourceElt, ResourceManager resourceManager) throws IOException {
    super(buildLexLoader(resourceElt, resourceManager));
  }

  private static final LexLoader buildLexLoader(DomElement resourceElt, ResourceManager resourceManager) {
    final File dbFileDir = resourceManager.getOptions().getFile("dbFileDir", "workingDir");
    final LexLoader lexLoader = new LexLoader(dbFileDir);
    return lexLoader;
  }

}
