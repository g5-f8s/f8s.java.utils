package org.g5.util;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GZipperTest {

	@Test
	void roundTripCompressionTest() throws Exception {
		String originalData = IOUtils.toString(new FileInputStream(getClass().getResource("/xml-spliterator.xml").getFile()));
		byte[] compressed = GZipper.compress(originalData);
		assertThat(compressed.length).isLessThan(375);
		assertThat(GZipper.decompress(compressed)).isEqualTo(originalData);
		
	}

}
