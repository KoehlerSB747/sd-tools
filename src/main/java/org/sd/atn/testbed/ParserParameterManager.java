package org.sd.atn.testbed;


import java.io.File;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sd.atn.AtnGrammar;
import org.sd.atn.AtnParseRunner;
import org.sd.atn.AtnParserWrapper;
import org.sd.atn.CompoundParser;
import org.sd.atn.ParseConfig;
import org.sd.atn.ResourceManager;
import org.sd.token.StandardTokenizerOptions;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Container for managing parameters and resources.
 * <p>
 * @author Spence Koehler
 */
public class ParserParameterManager {
  
  private AtnParseRunner parseRunner;
  private ParserParameterContainer parserContainer;
  private Map<String, ParserParameterContainer> tokenizerContainers;
  private Tree<ParserParameterContainer> paramTree;

  public ParserParameterManager(AtnParseRunner parseRunner) {
    this(parseRunner, "top");
  }

  public ParserParameterManager(AtnParseRunner parseRunner, String label) {
    this.parseRunner = parseRunner;
    this.tokenizerContainers = new LinkedHashMap<String, ParserParameterContainer>();
    this.paramTree = buildParamTree(label, parseRunner);
    this.parserContainer = paramTree.getData();
  }

  public AtnParseRunner getParseRunner() {
    return parseRunner;
  }

  public ParserParameterContainer getParserContainer() {
    return parserContainer;
  }

  public Map<String, ParserParameterContainer> getTokenizerContainers() {
    return tokenizerContainers;
  }

  public ParserParameterContainer[] getTokenizers() {
    return tokenizerContainers.values().toArray(new ParserParameterContainer[tokenizerContainers.size()]);
  }

  public ParserParameterContainer getTokenizerContainer(String label) {
    return tokenizerContainers.get(label);
  }

  public Tree<ParserParameterContainer> getParamTree() {
    return paramTree;
  }


  private final Tree<ParserParameterContainer> buildParamTree(String label, AtnParseRunner parseRunner) {

    // xParser
    //   configProperties
    //   supplement-1..N
    //   compoundParser-1..N
    //   resource-1..N


    // build "xParser" as root
    final ParseConfig parseConfig = parseRunner.getParseConfig();
    final DataProperties options = parseRunner.getOptions();
    final DataProperties parseConfigProperties = parseConfig.getParseConfigProperties();
    final List<DomElement> supplementElements = parseConfig.getSupplementElements();

    final String parseConfigPath = options.getString("parseConfig", "<unknown>");
    final String supplementalConfig = options.getString("supplementalConfig", "<none>"); // NOTE:semi-colon delimitted list

    final ParserParameterContainer rootContainer =
      new ParserParameterContainer(label,
                             new ResourceManager.XmlMetaData(options),
                             new String[][] {
                               {"parseConfig", parseConfigPath},
                               {"supplementalConfig", supplementalConfig},
                             });
    rootContainer.setParseRunner(parseRunner);
    
    final Tree<ParserParameterContainer> result = new Tree<ParserParameterContainer>(rootContainer);

    // build a child for supplements
    if (supplementElements != null) {
      int supplementNum = 1;
      for (DomElement supplementElement : supplementElements) {
        result.addChild(new ParserParameterContainer("supplement-" + (supplementNum++),
                                               new ResourceManager.XmlMetaData(supplementElement),
                                               null));
      }
    }

    // build a child for configProperties
    if (parseConfigProperties != null) {
      result.addChild(new ParserParameterContainer("configProperties",
                                             new ResourceManager.XmlMetaData(parseConfigProperties),
                                             null));
    }

    // build a child for each compoundParser
    final Map<String, CompoundParser> compoundParsers = parseConfig.getId2CompoundParser();
    if (compoundParsers != null) {
      for (Map.Entry<String, CompoundParser> compoundParserEntry : compoundParsers.entrySet()) {
        final String key = compoundParserEntry.getKey();
        final CompoundParser compoundParser = compoundParserEntry.getValue();
        final Tree<ParserParameterContainer> compoundParserNode = buildParamTree("compoundParser-" + key, compoundParser);
        result.addChild(compoundParserNode);
      }
    }

    // build a child for each meta-data item in the resourceManager
    final ResourceManager resourceManager = parseConfig.getResourceManager();
    if (resourceManager.hasMetaData()) {
      int resourceNum = 1;
      for (ResourceManager.MetaData metaData : resourceManager.getMetaData()) {
        result.addChild(new ParserParameterContainer("resource-" + (resourceNum++), metaData, null));
      }
    }

    return result;
  }

  private final Tree<ParserParameterContainer> buildParamTree(String label, CompoundParser compoundParser) {

    // compoundParser-X
    //   parser-1..N

    // build "compoundParser-X"
    final DataProperties config = compoundParser.getConfig();
    final ParserParameterContainer rootContainer =
      new ParserParameterContainer(label,
                             new ResourceManager.XmlMetaData(config),
                             null);

    final Tree<ParserParameterContainer> result = new Tree<ParserParameterContainer>(rootContainer);

    // build a child for each parser
    final Map<String, AtnParserWrapper> parserWrappers = compoundParser.getParserWrappers();
    if (parserWrappers != null) {
      for (Map.Entry<String, AtnParserWrapper> parserWrapperEntry : parserWrappers.entrySet()) {
        final String key = parserWrapperEntry.getKey();
        final AtnParserWrapper parserWrapper = parserWrapperEntry.getValue();
        final Tree<ParserParameterContainer> parserWrapperNode = buildParamTree("parser-" + key, parserWrapper);
        result.addChild(parserWrapperNode);
      }
    }

    return result;
  }

  private final Tree<ParserParameterContainer> buildParamTree(String label, AtnParserWrapper parserWrapper) {

    // parserWrapper
    //   parseOptions                new ResourceManager.XmlMetaData(parseOptions.getOptions())
    //   atnGrammar + supplements    atnGrammar.getGrammarNodes():List<DomElement>
    //   prequalifier
    //   parseSelector
    //   ambiguityResolver
    //   tokenizer

    // build "parser-X"
    final DomElement parserElement = parserWrapper.getParserElement();
    final ParserParameterContainer rootContainer =
      new ParserParameterContainer(label,
                             new ResourceManager.XmlMetaData(parserElement),
                             null);

    final Tree<ParserParameterContainer> result = new Tree<ParserParameterContainer>(rootContainer);

    // build parseOptions child
    final DataProperties parseOptions = parserWrapper.getParseOptions().getOptions();
    final ParserParameterContainer parseOptionsContainer =
      new ParserParameterContainer("parseOptions",
                             new ResourceManager.XmlMetaData(parseOptions),
                             null);
    result.addChild(parseOptionsContainer);

    // build grammar and grammar supplements children
    final AtnGrammar grammar = parserWrapper.getGrammar();
    if (grammar != null) {
      final List<DomElement> grammarNodes = grammar.getGrammarNodes();
      if (grammarNodes != null) {
        int supplementNum = 0;
        for (DomElement grammarNode : grammarNodes) {
          result.addChild(new ParserParameterContainer(supplementNum == 0 ? "grammar" : "grammarSupplement-" + supplementNum,
                                                 new ResourceManager.XmlMetaData(grammarNode),
                                                 null));
          ++supplementNum;
        }
      }
    }

    // build prequalifier child
    final DomElement prequalifierElement = parserWrapper.getPrequalifierElement();
    if (prequalifierElement != null) {
      result.addChild(new ParserParameterContainer("prequalifier",
                                             new ResourceManager.XmlMetaData(prequalifierElement),
                                             null));
    }

    // build parseSelector child
    final DomElement parseSelectorElement = parserWrapper.getParseSelectorElement();
    if (parseSelectorElement != null) {
      result.addChild(new ParserParameterContainer("parseSelector",
                                             new ResourceManager.XmlMetaData(parseSelectorElement),
                                             null));
    }

    // build ambiguityResolver child
    final DomElement ambiguityResolverElement = parserWrapper.getAmbiguityResolverElement();
    if (ambiguityResolverElement != null) {
      result.addChild(new ParserParameterContainer("ambiguityResolver",
                                             new ResourceManager.XmlMetaData(ambiguityResolverElement),
                                             null));
    }

    // build tokenizer child
    final DomElement tokenizerOverride = parserWrapper.getTokenizerOverride();
    if (tokenizerOverride != null) {
      result.addChild(new ParserParameterContainer("tokenizerOverride",
                                             new ResourceManager.XmlMetaData(tokenizerOverride),
                                             null));
    }
    else {
      final StandardTokenizerOptions tokenizerOptions = parserWrapper.getTokenizerOptions();
      if (tokenizerOptions != null) {
        final int tokenizerNum = tokenizerContainers.size() + 1;
        final String tokenizerLabel = "tokenizerOptions-" + tokenizerNum;

        final DomElement tokenizerXml = tokenizerOptions.asXml().getXmlElement();
        final ParserParameterContainer tokenizerContainer =
          new ParserParameterContainer(tokenizerLabel,
                                 new ResourceManager.XmlMetaData(tokenizerXml),
                                 null);
        tokenizerContainer.setTokenizerOptions(tokenizerOptions);
        result.addChild(tokenizerContainer);
        tokenizerContainers.put(tokenizerLabel, tokenizerContainer);
      }
    }

    return result;
  }
}
