package org.sunbird.content.textbook;

import static java.util.Optional.ofNullable;
import static org.sunbird.common.models.util.JsonKey.NAME;
import static org.sunbird.common.models.util.JsonKey.TEXT_TOC_FILE_SUPPRESS_COLUMN_NAMES;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;
import static org.sunbird.content.util.TextBookTocUtil.getObjectFrom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;

public class TextBookTocFileConfig {

  private static Map<String, Object> outputMapping =
      getObjectFrom(getConfigValue(JsonKey.TEXTBOOK_TOC_OUTPUT_MAPPING), Map.class);

  protected static Optional<Map<String, Object>> METADATA =
      ofNullable(outputMapping.get("metadata")).map(e -> (Map<String, Object>) e);

  protected static Optional<Map<String, Object>> HIERARCHY =
      ofNullable(outputMapping.get("hierarchy")).map(e -> (Map<String, Object>) e);

  protected static boolean SUPPRESS_EMPTY_COLUMNS =
      ofNullable(getConfigValue(TEXT_TOC_FILE_SUPPRESS_COLUMN_NAMES))
          .map(Boolean::parseBoolean)
          .orElse(false);

  protected static int LEVELS = HIERARCHY.map(Map::size).orElse(0);
  protected static String HIERARCHY_PROPERTY = NAME;

  protected static int metadataStartPos;
  protected static int hierarchyStartPos;

  protected static List<String> KEY_NAMES;
  protected static List<String> COLUMN_NAMES;
  protected static String[] COLUMN_NAMES_ARRAY;
  protected static List<String> COMPULSORY_COLUMNS_KEYS;
  protected static List<String> HIERARCHY_KEYS =
      HIERARCHY.map(h -> new ArrayList(h.keySet())).orElseGet(ArrayList::new);
  protected static List<String> ROW_METADATA;

  static {
    int currPos = 0;
    for (Entry e : outputMapping.entrySet()) {
      if (e.getValue() instanceof String) {
        currPos += 1;
      }
      if (e.getValue() instanceof Map) {
        if ("metadata".equals(e.getKey())) {
          metadataStartPos = currPos;
        }
        if ("hierarchy".equals(e.getKey())) {
          hierarchyStartPos = currPos;
        }
        currPos += ((Map<String, Object>) e.getValue()).size();
      }
      KEY_NAMES = keyNames();
      COLUMN_NAMES = columnNames();
      COLUMN_NAMES_ARRAY = COLUMN_NAMES.stream().toArray(String[]::new);
      COMPULSORY_COLUMNS_KEYS = compulsoryColumnsKeys();
      ROW_METADATA = rowMetadata();
    }
  }

  private static List keyNames() {
    List<String> keys = new ArrayList<>();
    for (Entry<String, Object> e : outputMapping.entrySet()) {
      if (e.getValue() instanceof String) {
        keys.add(e.getKey());
      }
      if (e.getValue() instanceof Map) {
        for (String s : ((Map<String, String>) e.getValue()).keySet()) {
          keys.add(s);
        }
      }
    }
    return keys;
  }

  private static List columnNames() {
    List<String> columns = new ArrayList<>();
    for (Entry<String, Object> e : outputMapping.entrySet()) {
      if (e.getValue() instanceof String) {
        columns.add((String) e.getValue());
      }
      if (e.getValue() instanceof Map) {
        for (Entry<String, String> entry : ((Map<String, String>) e.getValue()).entrySet()) {
          columns.add(entry.getValue());
        }
      }
    }
    return columns;
  }

  private static List compulsoryColumnsKeys() {
    List<String> keys = new ArrayList<>();
    for (Entry<String, Object> e : outputMapping.entrySet()) {
      if (StringUtils.equalsIgnoreCase("hierarchy", e.getKey())) {
        continue;
      }
      if (e.getValue() instanceof String) {
        keys.add(e.getKey());
      }
      if (e.getValue() instanceof Map) {
        for (Entry entry : ((Map<String, String>) e.getValue()).entrySet()) {
          keys.add((String) entry.getKey());
        }
      }
    }
    return keys;
  }

  private static List rowMetadata() {
    Set<String> props = new HashSet<>();
    for (Entry<String, Object> e : outputMapping.entrySet()) {
      if (StringUtils.equalsIgnoreCase("hierarchy", e.getKey())) {
        continue;
      }
      if (e.getValue() instanceof String) {
        props.add(e.getKey());
      }
      if (e.getValue() instanceof Map) {
        for (Entry<String, String> entry : ((Map<String, String>) e.getValue()).entrySet()) {
          props.add(entry.getKey());
        }
      }
    }
    props.addAll(COMPULSORY_COLUMNS_KEYS);
    return new ArrayList(props);
  }
}
