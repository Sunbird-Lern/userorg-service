package org.sunbird.auth.verifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.common.util.Time;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.util.PropertiesCache;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CryptoUtil.class, KeyManager.class, Base64Util.class, PropertiesCache.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class AccessTokenValidatorTest {
  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
  }

  @Test
  public void verifyUserAccessToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("iss", "nullrealms/null");
    payload.put("kid", "kid");
    payload.put("sub", "f:ca00376d-395f-aee687d7c8ad:10cca27c-2a13-443c-9e2b-c7d9589c1f5f");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(true);
    String userId =
        AccessTokenValidator.verifyUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            new HashMap<>());
    assertNotNull(userId);
  }

  @Test
  public void verifySourceUserAccessToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("iss", "urlrealms/master");
    payload.put("kid", "kid");
    payload.put("sub", "f:ca00376d-395f-aee687d7c8ad:10cca27c-2a13-443c-9e2b-c7d9589c1f5f");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(true);
    String userId =
        AccessTokenValidator.verifySourceUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            "url",
            new HashMap<>());
    assertNotNull(userId);
  }

  @Test
  public void verifySourceUserAccessToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("iss", "urlrealms/master");
    payload.put("kid", "kid");
    payload.put("sub", "f:ca00376d-395f-aee687d7c8ad:10cca27c-2a13-443c-9e2b-c7d9589c1f5f");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(true);
    String userId =
        AccessTokenValidator.verifySourceUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            "url");
    assertNotNull(userId);
  }

  @Test
  public void verifySourceUserAccessTokenInvalidToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("kid", "kid");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(false);
    String userId =
        AccessTokenValidator.verifySourceUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            "url");
    assertEquals("Unauthorized", userId);
  }

  @Test
  public void verifyUserAccessTokenInvalidToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("kid", "kid");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(false);
    String userId =
        AccessTokenValidator.verifyUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            new HashMap<>());
    assertEquals("Unauthorized", userId);
  }

  @Test
  public void verifyUserAccessTokenExpiredToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() - 3600000;
    payload.put("exp", expTime);
    payload.put("kid", "kid");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());
    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(true);
    String userId =
        AccessTokenValidator.verifyUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            new HashMap<>());
    assertEquals("Unauthorized", userId);
  }

  @Test
  public void verifyToken() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("requestedByUserId", "386c7960-7f85-4a24-8131-a8aba519ce7d");
    payload.put("requestedForUserId", "386c7960-7f85-4a24-8131-a8aba519ce7e");
    payload.put("kid", "kid");
    payload.put("parentId", "386c7960-7f85-4a24-8131-a8aba519ce7d");
    payload.put("sub", "386c7960-7f85-4a24-8131-a8aba519ce7e");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());

    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(true);
    String userId =
        AccessTokenValidator.verifyManagedUserToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
            "386c7960-7f85-4a24-8131-a8aba519ce7d",
            "386c7960-7f85-4a24-8131-a8aba519ce7e",
            new HashMap<>());
    assertNotNull(userId);
  }

  @Test
  public void verifyTokenWithNullParentId() throws JsonProcessingException {
    PowerMockito.mockStatic(CryptoUtil.class);
    PowerMockito.mockStatic(Base64Util.class);
    PowerMockito.mockStatic(KeyManager.class);
    KeyData keyData = PowerMockito.mock(KeyData.class);
    Mockito.when(KeyManager.getPublicKey(Mockito.anyString())).thenReturn(keyData);
    PublicKey publicKey = PowerMockito.mock(PublicKey.class);
    Mockito.when(keyData.getPublicKey()).thenReturn(publicKey);
    Map<String, Object> payload = new HashMap<>();
    int expTime = Time.currentTime() + 3600000;
    payload.put("exp", expTime);
    payload.put("requestedByUserId", "386c7960-7f85-4a24-8131-a8aba519ce7d");
    payload.put("requestedForUserId", "386c7960-7f85-4a24-8131-a8aba519ce7e");
    payload.put("kid", "kid");
    payload.put("sub", "386c7960-7f85-4a24-8131-a8aba519ce7e");
    ObjectMapper mapper = new ObjectMapper();
    Mockito.when(Base64Util.decode(Mockito.any(String.class), Mockito.anyInt()))
        .thenReturn(mapper.writeValueAsString(payload).getBytes());

    Mockito.when(
            CryptoUtil.verifyRSASign(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
        .thenReturn(true);
    try {
      AccessTokenValidator.verifyManagedUserToken(
          "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
          "386c7960-7f85-4a24-8131-a8aba519ce7d",
          "386c7960-7f85-4a24-8131-a8aba519ce7e",
          new HashMap<>());
    } catch (Exception e) {
      assertNotNull(e);
    }
  }
}
