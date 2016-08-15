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
package org.sd.wordnet.apps;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.wordnet.lex.LexDictionary;
import org.sd.wordnet.lex.LexLoader;
import org.sd.wordnet.token.SimpleWordLookupStrategy;
import org.sd.wordnet.token.TokenCollectionHandler;
import org.sd.wordnet.token.WordLookupStrategy;
import org.sd.wordnet.token.WordNetToken;
import org.sd.wordnet.token.WordNetTokenGenerator;

/**
 * Utility to find undefined words in a corpus.
 * <p>
 * @author Spencer Koehler
 */
public class UndefinedWordFinder {
  
  static boolean EMIT = true;
  static boolean EMIT_TOKENS = false;


  private LexDictionary dict;
  private WordNetTokenGenerator tokenGenerator;
  private WordLookupStrategy lookupStrategy;

  public UndefinedWordFinder(File dbFileDir) throws IOException {
    this.dict = new LexDictionary(new LexLoader(dbFileDir));
    this.tokenGenerator = new WordNetTokenGenerator(dict);
    this.lookupStrategy = new SimpleWordLookupStrategy(dict);
  }

  /** @return the number of words *not* found. */
  public void find(BufferedReader reader) throws IOException {
    String line = null;

    while ((line = reader.readLine()) != null) {
      find(line);
    }
  }

  /** @return the number of words *not* found. */
  public TokenCollectionHandler find(String line) {

    final TokenCollectionHandler tokenHandler = new TokenCollectionHandler();
    tokenGenerator.generate(tokenHandler, line, lookupStrategy);

    if (EMIT_TOKENS) {  // emit tokens
      if (EMIT) {
        System.out.println(tokenHandler);
      }
    }
    else {  // emit unknown (and tagged) words
      for (WordNetToken wnToken : tokenHandler.getTokens()) {
        if (wnToken.isUnknown()) {
          // undefined
          if (EMIT) {
            System.out.println(wnToken.getInput() + "\t" + wnToken.getNorm());
          }
        }
        else {
          // is defined
          if (wnToken.hasTags()) {
            if (EMIT) {
              final String tagsString = getTagsString(wnToken.getTags());
              System.err.println(tagsString + ": " + wnToken.getInput());
            }
          }
          else {  // has synsets
            if (EMIT) {
              final String label = getTagsString(wnToken.getCategories());
              System.err.println(label + ": " + wnToken.getNorm());
            }
          }
        }
      }
    }

    return tokenHandler;
  }

  private static final String getTagsString(Set<String> tags) {
    final StringBuilder result = new StringBuilder();

    if (tags != null) {
      for (String tag : tags) {
        if (result.length() > 0) result.append(", ");
        result.append(tag);
      }
    }

    return result.toString();
  }


  public static void main(String[] args) throws IOException {
    // arg0 -- path to dbFileDir
    // args1+ -- path to text files to search
    // stdout -- undefined words, one per line
    // stderr -- tagged words (undefined, but recognized)

    final UndefinedWordFinder finder = new UndefinedWordFinder(new File(args[0]));

    if (args.length == 1) {
      // read words from stdin
      final BufferedReader reader = FileUtil.getReader(System.in);
      finder.find(reader);
      reader.close();
    }
    else {
      for (int i = 1; i < args.length; ++i) {
        final String arg = args[i];
        final File argFile = new File(arg);
        if (argFile.exists()) {
          EMIT = true;
          // test each word in the file
          final BufferedReader reader = FileUtil.getReader(argFile);
          finder.find(reader);
          reader.close();
        }
        else {
          EMIT = false;
          // just test the command line arg as a word
          final TokenCollectionHandler tokenHandler = finder.find(arg);
          System.out.println(tokenHandler);
        }
      }
    }
  }
}
