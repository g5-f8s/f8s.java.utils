package org.f8s.g5.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {
	
	public static <T> Stream<T> sequential(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

}
