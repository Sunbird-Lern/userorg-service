package org.sunbird.validator.user;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.bean.MigrationUser;
import org.sunbird.bean.ShadowUserUpload;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.error.CsvError;
import org.sunbird.error.CsvRowErrorDetails;
import org.sunbird.error.ErrorEnum;
import org.sunbird.error.IErrorDispatcher;
import org.sunbird.error.factory.ErrorDispatcherFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * this class will validate the csv file for shadow db
 * @author anmolgupta
 */
public class UserBulkMigrationRequestValidator {

    private ShadowUserUpload shadowUserMigration;
    private HashSet<String> userExternalIdsSet=new HashSet<>();
    private CsvError csvRowsErrors=new CsvError();
    private static final int MAX_ROW_SUPPORTED=20000;
    private static final String NAME_REG_MATCHER="^[^.][^^;\\-()<>|!=’%_#$]+";

    private UserBulkMigrationRequestValidator(ShadowUserUpload migration) {
        this.shadowUserMigration = migration;
    }
    public static UserBulkMigrationRequestValidator getInstance(ShadowUserUpload migration){
        return new UserBulkMigrationRequestValidator(migration);
    }
    public void validate()
    {        ProjectLogger.log("UserBulkMigrationRequestValidator:validate:start validating migration users", LoggerEnum.INFO.name());
        checkCsvRows();
    }
    private void checkCsvRows(){
        validateRowsCount();
        shadowUserMigration.getValues().stream().forEach(onCsvRow -> {
            int index=shadowUserMigration.getValues().indexOf(onCsvRow);
            validateMigrationUser(onCsvRow,index);
        });
        if(csvRowsErrors.getErrorsList().size()>0){
            IErrorDispatcher errorDispatcher= ErrorDispatcherFactory.getErrorDispatcher(csvRowsErrors);
            errorDispatcher.dispatchError();
        }
    }

    private void validateRowsCount(){
        int ROW_BEGINNING_INDEX=1;
        if(shadowUserMigration.getValues().size()>=MAX_ROW_SUPPORTED){
            throw new ProjectCommonException(
                    ResponseCode.csvRowsExceeds.getErrorCode(),
                    ResponseCode.csvRowsExceeds.getErrorMessage().concat("supported:"+MAX_ROW_SUPPORTED),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        else if(shadowUserMigration.getValues().size()<ROW_BEGINNING_INDEX){
            throw new ProjectCommonException(
                    ResponseCode.noDataForConsumption.getErrorCode(),
                    ResponseCode.noDataForConsumption.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
    }
    private void validateMigrationUser(MigrationUser migrationUser,int index) {

        checkEmailAndPhone(migrationUser.getEmail(),migrationUser.getPhone(),index);
        checkUserExternalId(migrationUser.getUserExternalId(),index);
        checkName(migrationUser.getName(),index);
        checkInputStatus(migrationUser.getInputStatus(),index);
    }

    private void addErrorToList(CsvRowErrorDetails errorDetails){
        csvRowsErrors.setError(errorDetails);

    }

    public void checkEmailAndPhone(String email, String phone,int index) {
        CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
        errorDetails.setRowId(index);
        boolean isEmailBlank=StringUtils.isBlank(email);
        boolean isPhoneBlank=StringUtils.isBlank(phone);
        if(isEmailBlank && isPhoneBlank){
            errorDetails.setErrorEnum(ErrorEnum.missing);
            errorDetails.setHeader(JsonKey.EMAIL);
        }

        if(!isEmailBlank){
            checkEmail(email,index);
        }
        if(!isPhoneBlank){
            checkPhone(phone,index);
        }
        if(errorDetails.getErrorEnum()!=null) {
            addErrorToList(errorDetails);
        }
    }

    private void checkEmail(String email,int index){
        CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
        errorDetails.setRowId(index);
        errorDetails.setHeader(JsonKey.EMAIL);
        boolean isEmailValid=ProjectUtil.isEmailvalid(email);
        if(!isEmailValid){
            errorDetails.setErrorEnum(ErrorEnum.invalid);
        }
        if(errorDetails.getErrorEnum()!=null) {
            addErrorToList(errorDetails);
        }

    }

    private void checkPhone(String phone,int index){
        CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
        errorDetails.setRowId(index);
        errorDetails.setHeader(JsonKey.PHONE);
        boolean isPhoneValid=ProjectUtil.validatePhoneNumber(phone);
        if(!isPhoneValid){
            errorDetails.setErrorEnum(ErrorEnum.invalid);
        }
        if(errorDetails.getErrorEnum()!=null) {
        addErrorToList(errorDetails);
    }
    }

    private void checkUserExternalId(String userExternalId,int index) {
        CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
        errorDetails.setRowId(index);
        errorDetails.setHeader(JsonKey.USER_EXTERNAL_ID);
        if(StringUtils.isBlank(userExternalId)){
            errorDetails.setErrorEnum(ErrorEnum.missing);
        }
        if(!userExternalIdsSet.add(userExternalId)){
            errorDetails.setErrorEnum(ErrorEnum.duplicate);
        }
        if (errorDetails.getErrorEnum() != null) {
            addErrorToList(errorDetails);
        }
    }

    private void checkName(String name,int index) {
        if(StringUtils.isNotBlank(name) && !(Pattern.matches(NAME_REG_MATCHER, name))){
            CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
            errorDetails.setRowId(index);
            errorDetails.setHeader(JsonKey.NAME);
            errorDetails.setErrorEnum(ErrorEnum.invalid);
            addErrorToList(errorDetails);
        }
        checkValue(name,index,JsonKey.NAME);
    }


    private void checkInputStatus(String inputStatus, int index) {
        CsvRowErrorDetails errorDetails = new CsvRowErrorDetails();
        errorDetails.setRowId(index);
        if (StringUtils.isBlank(inputStatus)) {
            errorDetails.setHeader(JsonKey.INPUT_STATUS);
            errorDetails.setErrorEnum(ErrorEnum.missing);
            addErrorToList(errorDetails);

        } else if (!(inputStatus.equalsIgnoreCase(JsonKey.ACTIVE) ||
                inputStatus.equalsIgnoreCase(JsonKey.INACTIVE))) {
            errorDetails.setHeader(JsonKey.INPUT_STATUS);
            errorDetails.setErrorEnum(ErrorEnum.invalid);
            addErrorToList(errorDetails);
        }
}
    private void checkValue(String column, int rowIndex, String header){
        if(StringUtils.isBlank(column)) {
            CsvRowErrorDetails errorDetails=new CsvRowErrorDetails();
            errorDetails.setRowId(rowIndex);
            errorDetails.setHeader(header);
            errorDetails.setErrorEnum(ErrorEnum.missing);
            addErrorToList(errorDetails);
        }
    }
}