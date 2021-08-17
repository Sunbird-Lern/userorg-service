package org.sunbird.operations;

public enum OrganisationActorOperation {
    CREATE_ORG("createOrg"),
    UPDATE_ORG("updateOrg"),
    UPDATE_ORG_STATUS("updateOrgStatus"),
    GET_ORG_DETAILS("getOrgDetails"),
    ASSIGN_KEYS("assignKeys"),
    UPSERT_ORGANISATION_TO_ES("upsertOrganisationDataToES");

    private String value;

    OrganisationActorOperation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
