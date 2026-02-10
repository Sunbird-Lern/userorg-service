package org.sunbird.utils;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract factory and utility class for file operations.
 * Provides methods to write list data to files and instantiate specific file utilities.
 */
public abstract class FileUtil {

  /**
   * Abstract method to write data to a file.
   *
   * @param fileName The name of the file.
   * @param dataValues The data to be written.
   * @return The created File object.
   */
  public abstract File writeToFile(String fileName, List<List<Object>> dataValues);

  /**
   * Helper method to convert a List of objects into a comma-separated String.
   *
   * @param obj The object which must be a List.
   * @return A comma-separated string representation of the list, or empty string if empty.
   */
  @SuppressWarnings("unchecked")
  protected static String getListValue(Object obj) {
    List<Object> data = (List<Object>) obj;
    if (!(data.isEmpty())) {
      StringBuilder sb = new StringBuilder();
      for (Object value : data) {
        sb.append((String) value).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
    return "";
  }

  /**
   * Factory method to get a specific FileUtil implementation based on format.
   *
   * @param format The desired file format (e.g., "excel").
   * @return An instance of the corresponding FileUtil implementation.
   */
  public static FileUtil getFileUtil(String format) {
    String tempformat = "";
    if (!StringUtils.isBlank(format)) {
      tempformat = format.toLowerCase();
    }
    switch (tempformat) {
      case "excel":
        return (new ExcelFileUtil());
      default:
        return (new ExcelFileUtil());
    }
  }
}
