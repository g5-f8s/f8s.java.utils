package org.f8s.g5.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class GZipperTest {

	@Test
	public void roundTripCompressionTest() throws Exception {
		String originalData = IOUtils.toString(new FileInputStream(getClass().getResource("/xml-spliterator.xml").getFile()));
		byte[] compressed = GZipper.compress(originalData);
		assertThat(compressed.length, is(lessThan(375)));
		assertThat(GZipper.decompress(compressed), is(originalData));
		
	}

}
