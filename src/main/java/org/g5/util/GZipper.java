package org.g5.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * I am a simple utility class for compressing/uncompressing data using
 * the GZip format.
 * 
 * Source code licensed under the GNU GPL v3.0 or later.
 * 
 * @author gerard.fernandes@gmail.com
 *
 */
public class GZipper {

	public static byte[] compress(String data) throws IOException {
		ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
		ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
		GZIPOutputStream zipOutputStream = new GZIPOutputStream(compressedData);
		IOUtils.copy(input, zipOutputStream);
		IOUtils.closeQuietly(zipOutputStream);
		return compressedData.toByteArray();
	}
	
	public static String decompress(byte[] compressedData) throws IOException {
		GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressedData));
		ByteArrayOutputStream decompressedBytes = new ByteArrayOutputStream();
		IOUtils.copy(input, decompressedBytes);
		IOUtils.closeQuietly(decompressedBytes);
		return decompressedBytes.toString();
	}
	
}
