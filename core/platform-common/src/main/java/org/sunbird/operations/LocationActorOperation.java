package org.sunbird.common.models.util;

public enum LocationActorOperation {
  CREATE_LOCATION("createLocation"),
  UPDATE_LOCATION("updateLocation"),
  SEARCH_LOCATION("searchLocation"),
  DELETE_LOCATION("deleteLocation"),
  GET_RELATED_LOCATION_IDS("getRelatedLocationIds"),
  READ_LOCATION_TYPE("readLocationType"),
  UPSERT_LOCATION_TO_ES("upsertLocationDataToES"),
  DELETE_LOCATION_FROM_ES("deleteLocationDataFromES");

  private String value;

  LocationActorOperation(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
