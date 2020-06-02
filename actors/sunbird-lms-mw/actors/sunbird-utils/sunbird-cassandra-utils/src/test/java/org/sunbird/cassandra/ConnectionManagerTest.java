/*
package org.sunbird.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.when;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
*/
/** @author kirti. Junit test cases *//*

                                      @RunWith(PowerMockRunner.class)
                                      @PrepareForTest({
                                        Cluster.class,
                                        Metadata.class,
                                        Cluster.Builder.class,
                                        CassandraUtil.class,
                                        CassandraConnectionMngrFactory.class,
                                      })
                                      @PowerMockIgnore("javax.management.*")
                                      public class ConnectionManagerTest {

                                        private static PropertiesCache cach = PropertiesCache.getInstance();
                                        private static String host = cach.getProperty("contactPoint");
                                        private static String port = cach.getProperty("port");
                                        private static String cassandraKeySpace = cach.getProperty("keyspace");
                                        private static final Cluster.Builder builder = PowerMockito.mock(Cluster.Builder.class);
                                        private static Cluster cluster;
                                        private static Metadata metadata;
                                        private static Session session = PowerMockito.mock(Session.class);

                                        private static CassandraConnectionManagerImpl connectionManager =
                                            (CassandraConnectionManagerImpl)
                                                CassandraConnectionMngrFactory.getObject(
                                                    cach.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE));

                                        @BeforeClass
                                        public static void init() {

                                          PowerMockito.mockStatic(Cluster.class);
                                          cluster = PowerMockito.mock(Cluster.class);
                                          when(cluster.connect(Mockito.anyString())).thenReturn(session);
                                          metadata = PowerMockito.mock(Metadata.class);
                                          when(cluster.getMetadata()).thenReturn(metadata);
                                          when(Cluster.builder()).thenReturn(builder);
                                          when(builder.addContactPoint(Mockito.anyString())).thenReturn(builder);
                                          when(builder.withPort(Mockito.anyInt())).thenReturn(builder);
                                          when(builder.withProtocolVersion(Mockito.any())).thenReturn(builder);
                                          when(builder.withRetryPolicy(Mockito.any())).thenReturn(builder);
                                          when(builder.withTimestampGenerator(Mockito.any())).thenReturn(builder);
                                          when(builder.withPoolingOptions(Mockito.any())).thenReturn(builder);
                                          when(builder.build()).thenReturn(cluster);
                                          connectionManager.createConnection(host, port, "cassandra", "password", cassandraKeySpace);
                                        }

                                        @Before
                                        public void setUp() {

                                          reset(session);
                                          when(cluster.connect(Mockito.anyString())).thenReturn(session);
                                        }

                                        @Test
                                        public void testCreateConnectionSuccessWithoutUsernameAndPassword() throws Exception {

                                          boolean bool = connectionManager.createConnection(host, port, null, null, cassandraKeySpace);
                                          assertEquals(true, bool);
                                        }

                                        @Test
                                        public void testCreateConnectionSuccessWithUserNameAndPassword() throws Exception {

                                          Boolean bool =
                                              connectionManager.createConnection(host, port, "cassandra", "password", cassandraKeySpace);
                                          assertEquals(true, bool);
                                        }

                                        @Test
                                        public void testCreateConnectionFailure() {

                                          try {
                                            connectionManager.createConnection("127.0.0.1", "9042", "cassandra", "pass", "eySpace");
                                          } catch (Exception ex) {
                                          }
                                          assertTrue(500 == ResponseCode.SERVER_ERROR.getResponseCode());
                                        }
                                      }
                                      */
