package org.f8s.g5.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * I exist because Java-8 Streams are slightly bonkers in their API.
 * 
 * @author gerard.fernandes@gmail.com
 *
 */
public class Streams {
	
	/**
	 * I build a sequential stream over an iterable. Although this can be done
	 * with the StreamsSupport, the API isn't really very nice. For one thing,
	 * the iterables I expect here are going to be over large collections, and will
	 * be streaming in so that I don't have to hold the entire collection in memory
	 * at any point in time. For these case, access to the underlying iterable
	 * can not, realistically, be parallelised.
	 * 
	 * @param iterable
	 * @return
	 */
	public static <T> Stream<T> sequential(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

}
