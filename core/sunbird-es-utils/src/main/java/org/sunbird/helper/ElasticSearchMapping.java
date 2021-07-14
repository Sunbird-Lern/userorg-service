/** */
package org.sunbird.helper;

/**
 * This class will define Elastic search mapping.
 *
 * @author Manzarul
 */
public class ElasticSearchMapping {

  /**
   * This method will define ES default mapping.
   *
   * @return
   */
  public static String createMapping() {
    String mapping =
        "  {  \"dynamic_templates\": [ {\"longs\": {\"match_mapping_type\": \"long\", \"mapping\": {\"type\": \"long\", \"fields\": { \"raw\": {\"type\": \"long\"  } }}}},{\"booleans\": {\"match_mapping_type\": \"boolean\", \"mapping\": {\"type\": \"boolean\", \"fields\": { \"raw\": { \"type\": \"boolean\" }} }}},{\"doubles\": {\"match_mapping_type\": \"double\",\"mapping\": {\"type\": \"double\",\"fields\":{\"raw\": { \"type\": \"double\" } }}}},{ \"dates\": {\"match_mapping_type\": \"date\", \"mapping\": { \"type\": \"date\",\"fields\": {\"raw\": { \"type\": \"date\" } } }}},{\"strings\": {\"match_mapping_type\": \"string\",\"mapping\": {\"type\": \"text\",\"fielddata\": true,\"copy_to\": \"all_fields\",\"analyzer\": \"cs_index_analyzer\",\"search_analyzer\": \"cs_search_analyzer\",\"fields\": {\"raw\": {\"type\": \"text\",\"fielddata\": true,\"analyzer\": \"keylower\"}}}}}],\"properties\": {\"all_fields\": {\"type\": \"text\",\"analyzer\": \"cs_index_analyzer\",\"search_analyzer\": \"cs_search_analyzer\",\"fields\": { \"raw\": { \"type\": \"text\",\"analyzer\": \"keylower\" } }} }}";
    return mapping;
  }
}
