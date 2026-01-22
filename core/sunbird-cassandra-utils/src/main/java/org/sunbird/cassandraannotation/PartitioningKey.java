package org.sunbird.cassandraannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as part of the Cassandra partitioning key.
 * Partitioning keys determine how data is distributed across nodes in the cluster.
 * This annotation can be applied to fields or parameters.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PartitioningKey {}
