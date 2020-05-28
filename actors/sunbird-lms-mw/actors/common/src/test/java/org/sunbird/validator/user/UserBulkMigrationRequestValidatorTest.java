package org.sunbird.validator.user;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.bean.MigrationUser;
import org.sunbird.bean.ShadowUserUpload;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.List;

@PrepareForTest(UserBulkMigrationRequestValidator.class)
public class UserBulkMigrationRequestValidatorTest {
    private static final int MAX_ROW_SUPPORTED=20000;

    @Test
    public void testRowsCountFailureWithEmptyCSVFile() {
        List<MigrationUser> migrationUserList=new ArrayList<>();
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals(ResponseCode.noDataForConsumption.getErrorMessage(), e.getMessage());
        }
    }

    @Test
    public void testRowsCountFailureWithMoreCsvRowsSupported() {
        List<MigrationUser> migrationUserList=new ArrayList<>();
        for(int i=0;i<MAX_ROW_SUPPORTED+1;i++){
            MigrationUser migrationUser = new MigrationUser();
            migrationUser.setChannel("TN");
            migrationUserList.add(migrationUser);
        }
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("2024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals(ResponseCode.csvRowsExceeds.getErrorMessage().concat("supported:"+MAX_ROW_SUPPORTED), e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithBlankName(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setPhone("9876543210");
        migrationUser.setInputStatus(JsonKey.ACTIVE);
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setUserExternalId("user ext id");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column name:is missing ]", e.getMessage());
        }
    }


    @Test
    public void testShadowUserMigrationWithBlankEmail(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus(JsonKey.ACTIVE);
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setName("Shadow User Name");
        migrationUser.setUserExternalId("user ext id");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column email:is missing ]", e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithInvalidEmail(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus(JsonKey.ACTIVE);
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setEmail("wrongemail");
        migrationUser.setName("Shadow User Name");
        migrationUser.setUserExternalId("user ext id");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertEquals("[ In Row 1:the Column email:is invalid ]", e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithInvalidPhone(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus(JsonKey.ACTIVE);
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("987897");
        migrationUser.setName("Shadow User Name");
        migrationUser.setUserExternalId("user ext id");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column phone:is invalid ]", e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithBlankUserExtId(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus(JsonKey.ACTIVE);
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("9876543210");
        migrationUser.setName("Shadow User Name");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column userExternalId:is missing ]", e.getMessage());
        }
    }


    @Test
    public void testShadowUserMigrationWithBlankInputStatus(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("9876543210");
        migrationUser.setUserExternalId("any user ext id");
        migrationUser.setName("Shadow User Name");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column input status:is missing ]", e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithInvalidInputStatus(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus("wrong input status");
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("9876543210");
        migrationUser.setUserExternalId("any user ext id");
        migrationUser.setName("Shadow User Name");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 1:the Column input status:is invalid ]", e.getMessage());
        }
    }
    @Test
    public void testShadowUserMigrationWithDuplicateUserExtId(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus("Active");
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("9876543210");
        migrationUser.setUserExternalId("any user ext id");
        migrationUser.setName("Shadow User Name");
        migrationUserList.add(migrationUser);
        MigrationUser anotherMigrationUser = new MigrationUser();
        anotherMigrationUser.setChannel("TN");
        anotherMigrationUser.setInputStatus("Active");
        anotherMigrationUser.setOrgExternalId("org ext id");
        anotherMigrationUser.setPhone("9876543210");
        anotherMigrationUser.setUserExternalId("any user ext id");
        anotherMigrationUser.setName("Shadow User Name");
        migrationUserList.add(anotherMigrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            Assert.assertEquals("[ In Row 2:the Column userExternalId:is duplicate ]", e.getMessage());
        }
    }

    @Test
    public void testShadowUserMigrationWithInvalidName(){
        List<MigrationUser> migrationUserList=new ArrayList<>();
        MigrationUser migrationUser = new MigrationUser();
        migrationUser.setChannel("TN");
        migrationUser.setInputStatus("Active");
        migrationUser.setOrgExternalId("org ext id");
        migrationUser.setPhone("9876543210");
        migrationUser.setUserExternalId("any user ext id");
        migrationUser.setName("###Shadow User Name");
        migrationUserList.add(migrationUser);
        try {
            new ShadowUserUpload.ShadowUserUploadBuilder()
                    .setProcessId(ProjectUtil.generateUniqueId())
                    .setFileSize("1024")
                    .setValues(migrationUserList)
                    .validate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.assertEquals("[ In Row 1:the Column name:is invalid ]", e.getMessage());
        }
    }





}