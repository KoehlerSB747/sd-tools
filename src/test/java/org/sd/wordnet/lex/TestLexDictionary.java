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
package org.sd.wordnet.lex;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the LexDictionary class.
 * <p>
 * @author Spence Koehler
 */
public class TestLexDictionary extends TestCase {

  public TestLexDictionary(String name) {
    super(name);
  }
  

  public void testDistinguishingBetweenMultipleSynsets() {
    final StringLexLoader lexLoader = new StringLexLoader();

    lexLoader.add("verb.perception", new String[] {
        // verb.perception: feel, feel1, feel10, feel9
        "{ [ feel, noun.attribute:feel,+ noun.cognition:feeling1,+ ] [ sense, adj.all:perceptible^sensible,+ adj.all:sensible,+ adj.all:sensitive4,+ adj.all:sensitive1,+ noun.cognition:sense2,+ noun.cognition:sensation1,+ noun.cognition:sensation,+ noun.artifact:sensor,+ noun.act:sensing,+ ] perceive,@ frames: 8,9 (perceive by a physical sensation, e.g., coming from the skin or muscles; \"He felt the wind\"; \"She felt an object brushing her arm\"; \"He felt his flesh crawl\"; \"She felt the heat when she got out of the car\") }",
        "{ [ feel1, noun.state:feeling,+ ] experience1,@ frames: 8,11 (undergo passive experience of; \"We felt the effects of inflation\"; \"her fingers felt their way through the string quartet\"; \"she felt his contempt of her\") }",
        "{ [ feel10, noun.attribute:feel,+ ] verb.contact:feel,$ verb.contact:feel1,$ verb.contact:feel11,$ touch,* verb.contact:search,@ frames: 8,9 (grope or feel in search of something; \"He felt for his wallet\")}",
        "{ [ feel9, noun.state:feel,+ ] seem,@ frames: 6 (produce a certain impression; \"It feels nice to be home again\") }",
      });

    lexLoader.add("verb.emotion", new String[] {
        // verb.emotion: feel
        "{ [ feel, noun.feeling:feelings,+ noun.Tops:feeling,+ feel_for,^ frames: 5,6,22 ] [ experience, noun.cognition:experience,+ noun.event:experience,+ ] frames: 8 (undergo an emotional sensation or be in a particular state of mind; \"She felt resentful\"; \"He felt regret\") }",
      });

    lexLoader.add("verb.possession", new String[] {
        // verb.possession: feel
        "{ [ feel, noun.animal:feeler2,+ noun.animal:feeler1,+ ] find,@ frames: 8 (find by testing or cautious exploration; \"He felt his way around the dark room\") }",
      });

    lexLoader.add("verb.body", new String[] {
        // verb.body: feel
        "{ feel, verb.stative:be3,@ frames: 7 (be conscious of a physical, mental, or emotional state; \"My cold is gone--I feel fine today\"; \"She felt tired after the long hike\"; \"She felt sad after her loss\") }",
      });

    lexLoader.add("verb.state", new String[] {
        // noun.state: spirit
        "{ [ spirit, verb.change:spirit,+ verb.contact:spiritize,+ ] tone1, [ feel, verb.perception:feel9,+ verb.stative:feel,+ ] [ feeling, verb.perception:feel1,+ ] flavor, flavour, [ look, verb.perception:look1,+ ] smell, atmosphere1,@ (the general atmosphere of a place or situation and the effect that it has on people; \"the feel of the city excited him\"; \"a clergyman improved the tone of the meeting\"; \"it had the smell of treason\") }",
      });

    lexLoader.add("noun.act", new String[] {
        // noun.act: feel
        "{ [ feel, verb.contact:feel11,+ ] foreplay,@ (manual stimulation of the genital area for sexual pleasure; \"the girls hated it when he tried to sneak a feel\") }",
      });

    lexLoader.add("verb.cognition", new String[] {
        // verb.cognition: feel1, find10
        "{ [ feel1, noun.feeling:feelings,+ ] believe4,@ frames: 5,24,20,21 (have a feeling or perception about oneself in reaction to someone's behavior or attitude; \"She felt small and insignificant\"; \"You make me feel naked\"; \"I made the students feel different about themselves\")}",
        "{ find10, [ feel, noun.cognition:feel,+ noun.cognition:feeling3,+ ] verb.communication:find1,$ conclude,@ frames: 26 (come to believe on the basis of emotion, intuitions, or indefinite grounds; \"I feel that he doesn't like me\"; \"I find him to be obnoxious\"; \"I found the movie rather entertaining\")}",
      });
      
    lexLoader.add("noun.cognition", new String[] {
        // noun.cognition: feel
        "{ [ feel, verb.cognition:feel,+ ] awareness,@ (an intuitive awareness; \"he has a feel for animals\"; \"it's easy when you get the feel of it\") }",
      });

    lexLoader.add("noun.attribute", new String[] {
        // noun.attribute: tactile_property
        "{ tactile_property, [ feel, verb.contact:feel1,+ verb.perception:feel10,+ verb.contact:feel,+ verb.perception:feel,+ ] property,@ (a property perceived by touch) }",
      });

    lexLoader.add("verb.stative", new String[] {
        // verb.stative: feel
        "{ [ feel, noun.state:feel,+ ] verb.perception:feel,$ verb.perception:appear,@ frames: 6,7 (be felt or perceived in a certain way; \"The ground feels shaky\"; \"The sheets feel soft\")}",
      });

    lexLoader.add("verb.contact", new String[] {
        // verb.contact: feel, feel11, palpate
        "{ [ feel, noun.animal:feeler2,+ noun.animal:feeler1,+ noun.attribute:feel,+ noun.cognition:feeling2,+ ] [ finger5, noun.body:finger,+ noun.act:fingering,+ ] touch,* frames: 8 (examine by touch; \"Feel this soft cloth!\"; \"The customer fingered the sweater\") }",
        "{ [ feel11, noun.act:feel,+ ] touch,@ noun.communication:slang,;u frames: 9 (pass one's hands over the sexual organs of; \"He felt the girl in the movie theater\") }",
        "{ [ palpate, adj.pert:palpatory,+ noun.act:palpation,+ ] [ feel1, noun.attribute:feel,+ ] touch,@ noun.act:medicine,;c frames: 8 (examine (a body part) by palpation; \"The nurse palpated the patient's stomach\"; \"The runner felt her pulse\") }",
      });


    final LexDictionary lexDictionary = new LexDictionary(lexLoader);
    
    // grab words with verb frame=5 to ensure correct loading
    final Set<String> samples = new HashSet<String>();
    final List<Synset> synsets = lexDictionary.getSynsets().get("feel");
    for (Synset synset : synsets) {
      final boolean synsetHasFrames = synset.hasFrames();
      for (Word word : synset.getWords()) {
        if (word.hasFrames() || synset.hasFrames()) {
          if (word.getAllFrames().contains(5)) {
            samples.add(word.getQualifiedWordName());
          }
        }
      }
    }

    assertEquals(2, samples.size());
    assertTrue(samples.contains("verb.emotion:feel"));
    assertTrue(samples.contains("verb.cognition:feel1"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLexDictionary.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
