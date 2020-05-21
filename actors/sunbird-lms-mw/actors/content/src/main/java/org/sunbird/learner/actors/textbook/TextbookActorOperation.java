package org.sunbird.learner.actors.textbook;

/**
 * This enum holds actor operations for Textbook TOC API.
 *
 * @author gauraw
 */
public enum TextbookActorOperation {
  TEXTBOOK_TOC_UPLOAD("textbookTocUpload"),
  TEXTBOOK_TOC_URL("textbookTocUrl");

  private String value;

  TextbookActorOperation(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
