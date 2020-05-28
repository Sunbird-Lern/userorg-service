package org.sunbird.helper;

import org.junit.Assert;
import org.junit.Test;

public class ElasticSearchMappingTest {
	
	@Test
	public void testcreateMapping() {
		String mapping = ElasticSearchMapping.createMapping();
		Assert.assertNotNull(mapping);
	}

}
