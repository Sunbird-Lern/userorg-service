package org.sunbird.operations.lms;

/**
 * Enum representing various operations related to locations within the system.
 */
public enum LocationActorOperation {
  CREATE_LOCATION("createLocation"),
  UPDATE_LOCATION("updateLocation"),
  SEARCH_LOCATION("searchLocation"),
  DELETE_LOCATION("deleteLocation"),
  GET_RELATED_LOCATION_IDS("getRelatedLocationIds"),
  READ_LOCATION_TYPE("readLocationType"),
  UPSERT_LOCATION_TO_ES("upsertLocationDataToES"),
  DELETE_LOCATION_FROM_ES("deleteLocationDataFromES");

  private final String value;

  /**
   * Constructor for LocationActorOperation.
   *
   * @param value The string value associated with the operation.
   */
  LocationActorOperation(String value) {
    this.value = value;
  }

  /**
   * Retrieves the string value of the operation.
   *
   * @return The operation value string.
   */
  public String getValue() {
    return this.value;
  }
}
