package org.sunbird.content.textbook;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class FileExtension {

  private String type;
  private List<String> seperators;

  public FileExtension(String extension, List<String> seperators) {
    this.type = extension;
    this.seperators = seperators;
  }

  public String getExtension() {
    return type;
  }

  public String getDotExtension() {
    return "." + type;
  }

  public String getSeperator() {
    return getSeperator(0);
  }

  public String getSeperator(int i) {
    return seperators.get(i);
  }

  public enum Extension {
    CSV("csv", new String[] {","});

    private String extension;
    private String[] seperators;

    Extension(String extension, String[] seperators) {
      this.extension = extension;
      this.seperators = seperators;
    }

    public String getDotExtension() {
      return "." + extension;
    }

    public String getSeperator() {
      return getSeperator(0);
    }

    public String getSeperator(int i) {
      return seperators[i];
    }

    public FileExtension getFileExtension() {
      return new FileExtension(extension, new ArrayList<>(asList(seperators)));
    }
  }
}
