package org.sunbird.cassandraannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field as part of the Cassandra clustering key.
 * Clustering keys determine the order of data within a partition and enable efficient range queries.
 * This annotation can be applied to fields or parameters.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClusteringKey {}
