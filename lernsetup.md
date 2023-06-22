This setup file contains the instruction to set up the required configuration for sunbird-user org service in the development
environment

### Organisation Creation:

1. Creating a new custodian/root organisation is mandatory. so please ensure you get a 200 OK response after creation.
   Please make sure you have disabled the required flag to skip the channel validation in externalresource.properties
   file as below,
   channel_registration_disabled=true

```shell
curl --location --request POST '{{host}}/v1/org/create' \
--header 'Content-Type: application/json' \
--data-raw '<org_create_request_payload'
```

{{host}} has to be replaced with respective local host url and <org_create_request_payload> has to be replaced with the
latest payload of ORG_CREATE_API

### Organisation Type Configuration:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "orgTypeConfig",
"field": "orgTypeConfig",
"value": "{\"fields\":[{\"name\":\"School\",\"value\":2,\"description\":\"School\",\"displayName\":\"School\",\"flagNameList\":[\"isSchool\"]},{\"name\":\"Board\",\"value\":5,\"description\":\"Board\",\"displayName\":\"Board\",\"flagNameList\":[\"isBoard\"]}]}"
}
}'
```

### Org Profile Configuration:

```shell
curl --location --request POST 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "orgProfileConfig",
"field": "orgProfileConfig",
"value": "{\"csv\":{\"supportedColumns\":{\"SCHOOL NAME\":\"orgName\",\"BLOCK CODE\":\"locationCode\",\"STATUS\":\"status\",\"SCHOOL ID\":\"organisationId\",\"EXTERNAL ID\":\"externalId\",\"DESCRIPTION\":\"description\",\"ORGANISATION cTYPE\":\"organisationType\"}, \"outputColumns\": {\"organisationId\":\"SCHOOL ID\",\"orgName\":\"SCHOOL NAME\",\"locationCode\":\"BLOCK CODE\",\"locationName\":\"BLOCK NAME\",\"externalId\":\"EXTERNAL ID\",\"organisationType\":\"ORGANISATION TYPE\"}, \"outputColumnsOrder\":[\"organisationId\",\"orgName\",\"locationCode\", \"locationName\",\"externalId\",\"organisationType\"],\"mandatoryColumns\":[\"orgName\",\"locationCode\",\"status\",\"organisationType\"]}}"
}
}'
```

### Custodian channel configuration:

```shell
curl --location --request POST '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "custodianOrgChannel",
"field": "custodianOrgChannel",
"value": "Channel"
}
}'
```

### Custodian org id configuration:

```shell
curl --location --request POST 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "custodianOrgId",
"field": "custodianOrgId",
"value": "0137038836873134080"
}
}'
```

### Terms and Conditions configuration:

#### Basic TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "tncConfig",
"field": "tncConfig",
"value": "{\"latestVersion\":\"v12\",\"v12\":{\"url\":\"https://obj.stage.sunbirded.org/termsandcondtions/terms-and-conditions-v12.html\"}}"
}
}'
```

#### Org Admin TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "orgAdminTnc",
"field": "orgAdminTnc",
"value": "{\"latestVersion\":\"3.5.0\",\"3.5.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#administratorGuidelines\"}}"
}
}'
```

#### Groups TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "groupsTnc",
"field": "groupsTnc",
"value": "{\"latestVersion\":\"3.5.0\",\"3.5.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#groupGuidelines\"}}"
}
}'
```

#### Report Viewer TNC:

```shell
curl --location '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"request": {
"id": "reportViewerTnc",
"field": "reportViewerTnc",
"value": "{\"latestVersion\":\"4.0.0\",\"4.0.0\":{\"url\":\"https://sunbirdstagingpublic.blob.core.windows.net/termsandcondtions/terms-and-conditions-v9.html#administratorGuidelines\"}}"
}
}'
```

### User Profile Configuration

```shell
curl --location --request POST '{{host}}/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data-raw '{
"request": {
"id": "userProfileConfig",
"field": "userProfileConfig",
"value": "{\"fields\":[\"firstName\",\"lastName\",\"profileSummary\",\"avatar\",\"countryCode\",\"dob\",\"email\",\"gender\",\"grade\",\"language\",\"location\",\"phone\",\"subject\",\"userName\",\"webPages\",\"jobProfile\",\"address\",\"education\",\"skills\",\"badgeAssertions\"],\"publicFields\":[\"firstName\",\"lastName\",\"profileSummary\",\"userName\"],\"privateFields\":[\"email\",\"phone\"],\"csv\":{\"supportedColumns\":{\"NAME\":\"firstName\",\"MOBILE PHONE\":\"phone\",\"EMAIL\":\"email\",\"SCHOOL ID\":\"orgId\",\"USER_TYPE\":\"userType\",\"ROLES\":\"roles\",\"USER ID\":\"userId\",\"SCHOOL EXTERNAL ID\":\"orgExternalId\"},\"outputColumns\":{\"userId\":\"USER ID\",\"firstName\":\"NAME\",\"phone\":\"MOBILE PHONE\",\"email\":\"EMAIL\",\"orgId\":\"SCHOOL ID\",\"orgName\":\"SCHOOL NAME\",\"userType\":\"USER_TYPE\",\"orgExternalId\":\"SCHOOL EXTERNAL ID\"},\"outputColumnsOrder\":[\"userId\",\"firstName\",\"phone\",\"email\",\"organisationId\",\"orgName\",\"userType\",\"orgExternalId\"],\"mandatoryColumns\":[\"firstName\",\"userType\",\"roles\"]},\"read\":{\"excludedFields\":[\"avatar\",\"jobProfile\",\"address\",\"education\",\"webPages\",\"skills\"]},\"framework\":{\"fields\":[\"board\",\"gradeLevel\",\"medium\",\"subject\",\"id\"],\"mandatoryFields\":[\"id\"]}}"
}
}'
```

### SMS Template Configuration

```shell
curl --location 'localhost:9000/v1/system/settings/set' \
--header 'Content-Type: application/json' \
--data '{
"id": "smsTemplateConfig",
"field": "smsTemplateConfig",
"value": "{\"91SMS\":{\"OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"1307161224258194219\",\"OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"1307161253593694015\",\"Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.\":\"1307161253600214425\",\"Welcome to $instanceName. Your user account has now been created. Click on the link below to  set a password  and start using your account: $link\":\"1307161353857474082\",\"You can now access your diksha state teacher account using $phone. Please log out and login once again to see updated details.\":\"1307161353855560999\",\"VidyaDaan: Your nomination for $content has not been accepted. Thank you for your interest. Please login to https:\/\/vdn.diksha.gov.in for details.\":\"1307161353848661841\",\"VidyaDaan: Your nomination for $content is accepted. Please login to https:\/\/vdn.diksha.gov.in to start contributing content.\":\"1307161353863933335\",\"VidyaDaan: Your Content $content has not been approved by the project owner. Please login to https:\/\/vdn.diksha.gov.in for details.\":\"1307161353861214243\",\"VidyaDaan: Your Content $content has been approved by the project owner.\":\"1307161353859625404\",\"VidyaDaan: Your Content $contentName for the project $projectName has been approved by the project owner. Please login to $url for details.\":\"1307162444865933051\",\"VidyaDaan: Your Content $contentName for the project $projectName has been approved by the project owner with few changes. Please login to $url for details.\":\"1307162444868558038\",\"VidyaDaan: Your Content $contentName has not been accepted by your organization upon review. Please login to $url for details.\":\"1307162400992655061\",\"All your diksha usage details are merged into your accountAll your diksha usage details are merged into your account $installationName . The account $account has been deleted\":\"1307161353851530988\",\"Use OTP $otp to edit the contact details for your Diksha profile.\":\"1307163542373112822\"},\"NIC\":{\"NCERT: OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"1007162851000583212\",\"NCERT: OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\":\"1007162851007549778\",\"NCERT: Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.\":\"1007162851045686096\",\"NCERT: Welcome to $instanceName. Your user account has now been created. Click on the link below to  set a password  and start using your account: $link\":\"1007162805254274946\",\"NCERT: You can now access your diksha state teacher account using $phone. Please log out and login once again to see updated details.\":\"1007162849410876095\",\"NCERT: Your nomination for $content has not been accepted. Thank you for your interest. Please login to $url for details.\":\"1007162805271660929\",\"NCERT: Your nomination for $content is accepted. Please login to $url to start contributing content.\":\"1007162805276881827\",\"NCERT: Your Content $content has not been approved by the project owner. Please login to $url for details.\":\"1007162805282556398\",\"NCERT: Your Content $contentName for the project $projectName has been approved by the project owner. Please login to $url for details.\":\"1007162805293127426\",\"NCERT: Your Content $contentName for the project $projectName has been approved by the project owner with few changes. Please login to $url for details.\":\"1007162805289863491\",\"NCERT: Your Content $contentName has not been accepted by your organization upon review. Please login to $url for details.\":\"1007162805285679055\",\"NCERT: All your diksha usage details are merged into your account $installationName . The account $account has been deleted\":\"1007162851061503958\"}}"
}'
```

#### System Env variables for sms template configuration

```shell
export sunbird_msg_91_auth=
export sunbird_msg_sender=
export  sunbird_installation_display_name_for_sms=
```

### Post configuration step

Once setup is complete, please refer to [keycloak_local_setup](keycloak_local_setup/keycloak_local_setup.md)

**Note:** 
{{host}} has to be replaced with respective local host url and <user_create_data> has to be replaced with the latest
payload of USER_CREATE_API
If you are able to create an user successfully then the local setup is working normal.