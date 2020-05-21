package org.sunbird.content.textbook;

import static java.io.File.separator;
import static java.util.Objects.nonNull;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.sunbird.common.models.util.JsonKey.CHILDREN;
import static org.sunbird.common.models.util.JsonKey.CONTENT_MIME_TYPE_COLLECTION;
import static org.sunbird.common.models.util.JsonKey.CONTENT_PROPERTY_MIME_TYPE;
import static org.sunbird.common.models.util.JsonKey.CONTENT_PROPERTY_VISIBILITY;
import static org.sunbird.common.models.util.JsonKey.CONTENT_PROPERTY_VISIBILITY_PARENT;
import static org.sunbird.common.models.util.JsonKey.IDENTIFIER;
import static org.sunbird.common.models.util.JsonKey.VERSION_KEY;
import static org.sunbird.common.models.util.LoggerEnum.ERROR;
import static org.sunbird.common.models.util.LoggerEnum.INFO;
import static org.sunbird.common.models.util.ProjectLogger.log;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;
import static org.sunbird.common.responsecode.ResponseCode.SERVER_ERROR;
import static org.sunbird.common.responsecode.ResponseCode.errorProcessingRequest;
import static org.sunbird.content.textbook.FileExtension.Extension.CSV;
import static org.sunbird.content.textbook.TextBookTocFileConfig.COLUMN_NAMES;
import static org.sunbird.content.textbook.TextBookTocFileConfig.COLUMN_NAMES_ARRAY;
import static org.sunbird.content.textbook.TextBookTocFileConfig.COMPULSORY_COLUMNS_KEYS;
import static org.sunbird.content.textbook.TextBookTocFileConfig.HIERARCHY;
import static org.sunbird.content.textbook.TextBookTocFileConfig.HIERARCHY_KEYS;
import static org.sunbird.content.textbook.TextBookTocFileConfig.HIERARCHY_PROPERTY;
import static org.sunbird.content.textbook.TextBookTocFileConfig.KEY_NAMES;
import static org.sunbird.content.textbook.TextBookTocFileConfig.LEVELS;
import static org.sunbird.content.textbook.TextBookTocFileConfig.ROW_METADATA;
import static org.sunbird.content.textbook.TextBookTocFileConfig.SUPPRESS_EMPTY_COLUMNS;
import static org.sunbird.content.util.ContentCloudStore.upload;
import static org.sunbird.content.util.TextBookTocUtil.getObjectFrom;
import static org.sunbird.content.util.TextBookTocUtil.stringify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;

public class TextBookTocUploader {
  public static final String TEXTBOOK_TOC_FOLDER = separator + "textbook" + separator + "toc";
  private Set<String> viewableColumns;

  private String textBookTocFileName;
  private FileExtension fileExtension;

  private Map<String, Object> row;

  private List<Map<String, Object>> rows = new ArrayList<>();
  private List<Map<String, Object>> parentChildHierarchyMapList = new ArrayList<>();

  public TextBookTocUploader(String textBookTocFileName, FileExtension fileExtension) {
    this.textBookTocFileName = textBookTocFileName;
    this.fileExtension = null == fileExtension ? CSV.getFileExtension() : fileExtension;
    if (SUPPRESS_EMPTY_COLUMNS) {
      viewableColumns = new HashSet<>();
      viewableColumns.addAll(COMPULSORY_COLUMNS_KEYS);
    }
  }

  public String execute(Map<String, Object> content, String textbookId, String versionKey) {

    if (!HIERARCHY.filter(h -> 0 != h.size()).isPresent()) return "";
    parentChildHierarchyMapList =
        getParentChildHierarchy(
            textbookId, (List<Map<String, Object>>) content.get(JsonKey.CHILDREN));
    log(
        "Creating CSV for TextBookToC | Id: " + textbookId + "Version Key: " + versionKey,
        INFO.name());
    File file = null;
    try {
      file = new File(this.textBookTocFileName + fileExtension.getDotExtension());
      deleteQuietly(file);
      log("Creating file for CSV at Location: " + file.getAbsolutePath(), INFO.name());
      touch(file);
      Instant startTime = Instant.now();
      populateDataIntoFile(content, file);
      log(
          "Timed:TextBookTocUploader:execute time taken in processing "
              + (Instant.now().getEpochSecond() - startTime.getEpochSecond()),
          INFO.name());
      log(
          "Uploading "
              + fileExtension.getExtension()
              + " to Cloud Storage for TextBookToC | Id: "
              + textbookId
              + ", Version Key: "
              + versionKey,
          INFO.name());
      return upload(TEXTBOOK_TOC_FOLDER, file);
    } catch (IOException e) {
      log(
          "Error creating "
              + fileExtension.getExtension()
              + " File at File Path | "
              + file.getAbsolutePath(),
          ERROR.name());
      throw new ProjectCommonException(
          errorProcessingRequest.getErrorCode(),
          errorProcessingRequest.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    } finally {
      log(
          "Deleting "
              + fileExtension.getExtension()
              + " for TextBookToC | Id: "
              + textbookId
              + ", "
              + "Version Key: "
              + versionKey);
      try {
        if (null != file && file.exists()) file.delete();
      } catch (SecurityException e) {
        log("Error! While deleting the local csv file: " + file.getAbsolutePath(), ERROR.name());
      } catch (Exception e) {
        log(
            "Error! Something Went wrong while deleting csv file: " + file.getAbsolutePath(),
            ERROR.name());
      }
    }
  }

  private void populateDataIntoFile(Map<String, Object> content, File file) {
    OutputStreamWriter out = null;
    CSVPrinter printer = null;
    try {
      if (SUPPRESS_EMPTY_COLUMNS) {
        log("Processing Hierarchy for TextBook | Id: " + content.get(IDENTIFIER), INFO.name());
        processHierarchySuppressColumns(content);

        String[] columns =
            IntStream.range(0, KEY_NAMES.size())
                .mapToObj(
                    i -> {
                      if (viewableColumns.contains(KEY_NAMES.get(i))) return COLUMN_NAMES.get(i);
                      else return null;
                    })
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        out.write(ByteOrderMark.UTF_BOM);
        log(
            "Writing Headers to Output Stream for Textbook | Id " + content.get(IDENTIFIER),
            INFO.name());
        printer = new CSVPrinter(out, DEFAULT.withHeader(columns));

        log(
            "Writing Data to Output Stream for Textbook | Id " + content.get(IDENTIFIER),
            INFO.name());
        for (Map<String, Object> row : rows) {
          updateBGMSData(row, content);
          Object[] tempRow =
              IntStream.range(0, KEY_NAMES.size())
                  .mapToObj(
                      i -> {
                        if (viewableColumns.contains(KEY_NAMES.get(i))) {
                          Object o = row.get(KEY_NAMES.get(i));
                          return null == o ? "" : o;
                        }
                        return null;
                      })
                  .filter(Objects::nonNull)
                  .toArray(Object[]::new);
          printer.printRecord(tempRow);
        }

        log(
            "Flushing Data to File | Location:"
                + file.getAbsolutePath()
                + " | for TextBook  | Id: "
                + content.get(IDENTIFIER));
      } else {
        log("Processing Hierarchy for TextBook | Id: " + content.get(IDENTIFIER), INFO.name());
        processHierarchy(content);

        out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        out.write(ByteOrderMark.UTF_BOM);

        log(
            "Writing Headers to Output Stream for Textbook | Id " + content.get(IDENTIFIER),
            INFO.name());
        printer = new CSVPrinter(out, DEFAULT.withHeader(COLUMN_NAMES_ARRAY));

        log(
            "Writing Data to Output Stream for Textbook | Id " + content.get(IDENTIFIER),
            INFO.name());
        for (Map<String, Object> row : rows) {
          updateBGMSData(row, content);
          Object[] tempRow =
              IntStream.range(0, KEY_NAMES.size())
                  .mapToObj(i -> row.get(KEY_NAMES.get(i)))
                  .toArray(Object[]::new);

          printer.printRecord(tempRow);
        }
      }
    } catch (IOException e) {
      log(
          "Error writing data to file | TextBook Id:"
              + content.get(IDENTIFIER)
              + "Version Key: "
              + content.get(VERSION_KEY),
          ERROR.name());
      throw new ProjectCommonException(
          errorProcessingRequest.getErrorCode(),
          errorProcessingRequest.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    } finally {
      log(
          "Flushing Data to File | Location:"
              + file.getAbsolutePath()
              + " | for TextBook  | Id: "
              + content.get(IDENTIFIER),
          INFO.name());
      try {
        if (nonNull(printer)) {
          printer.close();
        }
        if (nonNull(out)) {
          out.close();
        }
      } catch (IOException e) {
        log(
            "Error writing data to file | TextBook Id:"
                + content.get(IDENTIFIER)
                + "Version Key: "
                + content.get(VERSION_KEY),
            ERROR.name());
      }
    }
  }

  public void initializeRow() {
    row = new HashMap<>();
  }

  private String updateRowWithData(Map<String, Object> content, String key, int hierarchyLevel) {
    String k = (-1 == hierarchyLevel) ? key : HIERARCHY_KEYS.get(hierarchyLevel);
    if (null == content || null == content.get(key)) {
      row.remove(k);
    } else {
      row.put(k, stringify(content.get(key)));
    }
    return k;
  }

  private void processHierarchy(Map<String, Object> contentHierarchy) {
    initializeRow();
    int level = 0;
    updateRowWithData(contentHierarchy, HIERARCHY_PROPERTY, level);
    processHierarchyRecursive(contentHierarchy, level);
  }

  private void processHierarchyRecursive(Map<String, Object> contentHierarchy, int level) {
    List<Map<String, Object>> children = (List<Map<String, Object>>) contentHierarchy.get(CHILDREN);
    if (null != children && !children.isEmpty()) {
      if (LEVELS - 1 == level) return;
      for (Map<String, Object> child : children) {
        if (equalsIgnoreCase(
                CONTENT_PROPERTY_VISIBILITY_PARENT, (String) child.get(CONTENT_PROPERTY_VISIBILITY))
            && StringUtils.equals(
                CONTENT_MIME_TYPE_COLLECTION,
                (String) contentHierarchy.get(CONTENT_PROPERTY_MIME_TYPE))) {
          updateMetadata(child, ++level);
          appendRow();
          processHierarchyRecursive(child, level);
          updateMetadata(null, level--);
        }
      }
    }
  }

  private void updateMetadata(Map<String, Object> content, int level) {
    updateRowWithData(content, HIERARCHY_PROPERTY, level);
    for (String e : ROW_METADATA) {
      updateRowWithData(content, e, -1);
    }
    updateRowWithLinkedContent();
  }

  @SuppressWarnings("unchecked")
  private void updateRowWithLinkedContent() {
    String identifier = (String) row.get(JsonKey.IDENTIFIER);
    if (StringUtils.isNotBlank(identifier)) {
      Optional<Map<String, Object>> contentMap =
          parentChildHierarchyMapList
              .stream()
              .filter(
                  s -> {
                    for (Entry<String, Object> entry : s.entrySet()) {
                      if (identifier.equalsIgnoreCase(entry.getKey())) {
                        return true;
                      }
                    }
                    return false;
                  })
              .findFirst();
      if (contentMap.isPresent()) {
        Map<String, Object> childrenMap = contentMap.get();
        List<Map<String, Object>> children =
            (List<Map<String, Object>>)
                ((Map<String, Object>) childrenMap.get(identifier)).get(JsonKey.CHILDREN);
        AtomicInteger linkedContent = new AtomicInteger(1);
        children
            .stream()
            .filter(
                s ->
                    !(JsonKey.TEXTBOOK.equalsIgnoreCase((String) s.get(JsonKey.CONTENT_TYPE))
                        || JsonKey.TEXTBOOK_UNIT.equalsIgnoreCase(
                            (String) s.get(JsonKey.CONTENT_TYPE))))
            .sorted(
                (s, p) -> {
                  return (int) s.get(JsonKey.INDEX) - (int) p.get(JsonKey.INDEX);
                })
            .forEach(
                s -> {
                  String key =
                      MessageFormat.format(
                          ProjectUtil.getConfigValue(
                              JsonKey.SUNBIRD_TOC_LINKED_CONTENT_COLUMN_NAME),
                          linkedContent.getAndAdd(1));
                  if (ROW_METADATA.contains(key)) {
                    row.put(key, (String) s.get(JsonKey.IDENTIFIER));
                  }
                });
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getParentChildHierarchy(
      String parentId, List<Map<String, Object>> children) {
    List<Map<String, Object>> hierarchyList = new ArrayList<>();
    Map<String, Object> hierarchy = new HashMap<>();
    Map<String, Object> node = new HashMap<>();
    List<Map<String, Object>> contentIdList = new ArrayList<>();
    node.put(JsonKey.CHILDREN, contentIdList);
    hierarchy.put(parentId, node);
    hierarchyList.add(hierarchy);
    for (Map<String, Object> child : children) {
      Map<String, Object> contentIds = new HashMap<>();
      contentIds.put(JsonKey.IDENTIFIER, child.get(JsonKey.IDENTIFIER));
      contentIds.put(JsonKey.INDEX, child.get(JsonKey.INDEX));
      contentIds.put(JsonKey.CONTENT_TYPE, child.get(JsonKey.CONTENT_TYPE));
      contentIdList.add(contentIds);
      if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) child.get(JsonKey.CHILDREN))) {
        hierarchyList.addAll(
            getParentChildHierarchy(
                (String) child.get(JsonKey.IDENTIFIER),
                (List<Map<String, Object>>) child.get(JsonKey.CHILDREN)));
      }
    }
    return hierarchyList;
  }

  private void appendRow() {
    Map<String, Object> tempRow = new HashMap<>();
    tempRow.putAll(row);
    rows.add(tempRow);
  }

  private void processHierarchySuppressColumns(Map<String, Object> contentHierarchy) {
    initializeRow();
    int level = 0;
    updateRowWithDataSuppressColumns(contentHierarchy, HIERARCHY_PROPERTY, level);
    processHierarchyRecursiveSuppressColumns(contentHierarchy, level);
  }

  private void processHierarchyRecursiveSuppressColumns(
      Map<String, Object> contentHierarchy, int level) {
    List<Map<String, Object>> children = (List<Map<String, Object>>) contentHierarchy.get(CHILDREN);
    if (null != children && !children.isEmpty()) {
      if (LEVELS - 1 == level) return;
      for (Map<String, Object> child : children) {
        if (equalsIgnoreCase(
                CONTENT_PROPERTY_VISIBILITY_PARENT, (String) child.get(CONTENT_PROPERTY_VISIBILITY))
            && StringUtils.equals(
                CONTENT_MIME_TYPE_COLLECTION,
                (String) contentHierarchy.get(CONTENT_PROPERTY_MIME_TYPE))) {
          updateMetadataSuppressColumns(child, ++level);
          appendRow();
          processHierarchyRecursiveSuppressColumns(child, level);
          updateMetadataSuppressColumns(null, level--);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void updateBGMSData(Map<String, Object> row, Map<String, Object> contentHierarchy) {
    Map<String, Object> outputMapping =
        getObjectFrom(getConfigValue(JsonKey.TEXTBOOK_TOC_OUTPUT_MAPPING), Map.class);
    Map<String, Object> frameworkCategories =
        (Map<String, Object>) outputMapping.get("frameworkCategories");
    for (Entry<String, Object> entry : frameworkCategories.entrySet()) {
      String key = entry.getKey();
      if (null == contentHierarchy.get(key)) {
        row.put(key, "");
      } else {
        row.put(key, stringify(contentHierarchy.get(key)));
      }
    }
  }

  private void updateRowWithDataSuppressColumns(
      Map<String, Object> content, String key, int hierarchyLevel) {
    String k = updateRowWithData(content, key, hierarchyLevel);
    if (row.containsKey(k)) {
      viewableColumns.add(k);
    }
  }

  private void updateMetadataSuppressColumns(Map<String, Object> content, int level) {
    updateRowWithDataSuppressColumns(content, HIERARCHY_PROPERTY, level);
    for (String e : ROW_METADATA) {
      updateRowWithDataSuppressColumns(content, e, -1);
    }
    updateRowWithLinkedContent();
  }
}
