/** */
package org.sunbird.helper;

/**
 * This class will define Elastic search default settings.
 *
 * @author Manzarul
 */
public class ElasticSearchSettings {

  /**
   * This method will do default settings for Elastic search index
   *
   * @return String
   */
  public static String createSettingsForIndex() {
    String settings = 
        "{\"analysis\": {\"analyzer\": {\"cs_index_analyzer\": {\"type\": \"custom\",\"tokenizer\": \"standard\",\"filter\": [\"lowercase\",\"mynGram\"]},\"cs_search_analyzer\": {\"type\": \"custom\",\"tokenizer\": \"standard\",\"filter\": [\"lowercase\",\"standard\"]},\"keylower\": {\"type\": \"custom\",\"tokenizer\": \"keyword\",\"filter\": \"lowercase\"}},\"filter\": {\"mynGram\": {\"type\": \"ngram\",\"min_gram\": 1,\"max_gram\": 20,\"token_chars\": [\"letter\", \"digit\",\"whitespace\",\"punctuation\",\"symbol\"]} }}}";
    return settings;
  }
}
