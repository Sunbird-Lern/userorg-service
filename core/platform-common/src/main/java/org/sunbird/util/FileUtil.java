package org.sunbird.common.models.util;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public abstract class FileUtil {

  public abstract File writeToFile(String fileName, List<List<Object>> dataValues);

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
