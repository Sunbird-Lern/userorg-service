package org.sunbird.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelFileUtil extends FileUtil {

  private static LoggerUtil logger = new LoggerUtil(ExcelFileUtil.class);

  @SuppressWarnings({"resource", "unused"})
  public File writeToFile(String fileName, List<List<Object>> dataValues) {
    // Blank workbook
    XSSFWorkbook workbook = new XSSFWorkbook();
    // Create a blank sheet
    XSSFSheet sheet = workbook.createSheet("Data");
    FileOutputStream out = null;
    File file = null;
    int rownum = 0;
    for (Object key : dataValues) {
      Row row = sheet.createRow(rownum);
      List<Object> objArr = dataValues.get(rownum);
      int cellnum = 0;
      for (Object obj : objArr) {
        Cell cell = row.createCell(cellnum++);
        if (obj instanceof String) {
          cell.setCellValue((String) obj);
        } else if (obj instanceof Integer) {
          cell.setCellValue((Integer) obj);
        } else if (obj instanceof List) {
          cell.setCellValue(getListValue(obj));
        } else if (obj instanceof Double) {
          cell.setCellValue((Double) obj);
        } else {
          if (ProjectUtil.isNotNull(obj)) {
            cell.setCellValue(obj.toString());
          }
        }
      }
      rownum++;
    }

    try {
      // Write the workbook in file system
      file = new File(fileName + ".xlsx");
      out = new FileOutputStream(file);
      workbook.write(out);
      logger.info("File " + fileName + " created successfully");

    } catch (Exception e) {
      logger.error("writeToFile: " + e.getMessage(), e);
    } finally {
      if (null != out) {
        try {
          out.close();
        } catch (IOException e) {
          logger.error("writeToFile: " + e.getMessage(), e);
        }
      }
    }
    return file;
  }
}
