package org.sunbird.learner.actors.search;

import com.intuit.fuzzymatcher.component.MatchService;
import com.intuit.fuzzymatcher.domain.Document;
import com.intuit.fuzzymatcher.domain.Element;
import com.intuit.fuzzymatcher.domain.ElementType;
import com.intuit.fuzzymatcher.domain.Match;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;

public class FuzzyMatcher {

  private static final String nameToBeMatchedId = "0";
  private static final String ENCODING = "UTF-8";

  public static List<String> matchDoc(
      String nameToBeMatched, Map<String, String> attributesValueMap) {
    Document doc = null;
    try {
      doc =
          new Document.Builder(nameToBeMatchedId)
              .addElement(
                  new Element.Builder()
                      .setType(ElementType.TEXT)
                      .setValue(URLEncoder.encode(nameToBeMatched, ENCODING))
                      .createElement())
              .setThreshold(getFuzzyThreshold())
              .createDocument();
    } catch (UnsupportedEncodingException e) {
      ProjectLogger.log(
          "FuzzyMatcher:matchDoc: Error occured dueing encoding of data " + e,
          LoggerEnum.ERROR.name());
    }
    return match(doc, prepareDocumentFromSearchMap(attributesValueMap));
  }

  private static List<String> match(Document doc, List<Document> docList) {
    List<String> matchedKeys = new ArrayList<>();
    MatchService matchService = new MatchService();
    Map<Document, List<Match<Document>>> map = matchService.applyMatch(doc, docList);
    Iterator<Map.Entry<Document, List<Match<Document>>>> itr = map.entrySet().iterator();
    List<Match<Document>> matchList = null;
    while (itr.hasNext()) {
      Map.Entry<Document, List<Match<Document>>> entry = itr.next();
      matchList = entry.getValue();
      for (int i = 0; i < matchList.size(); i++) {
        Match<Document> matchDoc = matchList.get(i);
        matchedKeys.add(matchDoc.getMatchedWith().getKey());
        ProjectLogger.log(
            String.format(
                "%s:%s:document matched doc: %s with id  %s",
                "FuzzyMatcher", "match", matchDoc, matchDoc.getMatchedWith().getKey()),
            LoggerEnum.INFO.name());
      }
    }
    return matchedKeys;
  }

  private static List<Document> prepareDocumentFromSearchMap(
      Map<String, String> attributesValueMap) {
    List<Document> docList = new ArrayList<>();
    attributesValueMap
        .entrySet()
        .stream()
        .forEach(
            result -> {
              String[] attributes = result.getValue().split(" ");
              ProjectLogger.log(
                  "FuzzyMatcher:prepareDocumentFromSearchMap: the name got for match "
                      .concat(result.getValue() + "")
                      .concat("spliited name size is " + attributes.length),
                  LoggerEnum.INFO.name());
              for (int i = 0; i < attributes.length; i++) {
                try {
                  docList.add(
                      new Document.Builder(result.getKey())
                          .addElement(
                              new Element.Builder()
                                  .setType(ElementType.TEXT)
                                  .setValue(URLEncoder.encode(attributes[i].trim(), ENCODING))
                                  .createElement())
                          .createDocument());
                } catch (UnsupportedEncodingException e) {
                  ProjectLogger.log(
                      "Error occured during prepareDocumentFromSearchMap " + e,
                      LoggerEnum.ERROR.name());
                }
              }
            });
    ProjectLogger.log(
        String.format(
            "%s:%s:document size prepared to be matched is %s ",
            "FuzzyMatcher", "prepareDocumentFromSearchMap", docList.size()),
        LoggerEnum.INFO.name());
    return docList;
  }

  private static float getFuzzyThreshold() {
    String threshold =
        PropertiesCache.getInstance().readProperty(JsonKey.SUNBIRD_FUZZY_SEARCH_THRESHOLD);
    ProjectLogger.log(
        String.format(
            "%s:%s:the threshold got for Fuzzy search is %s",
            "FuzzyMatcher", "getFuzzyThreshold", threshold),
        LoggerEnum.INFO.name());
    return Float.parseFloat(threshold);
  }
}
