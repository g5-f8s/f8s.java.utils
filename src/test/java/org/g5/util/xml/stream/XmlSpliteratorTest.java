package org.g5.util.xml.stream;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.jdom2.Element;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.g5.util.Streams.sequential;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * 
 * Source code licensed under the GNU GPL v3.0 or later.
 *
 */
public class XmlSpliteratorTest {
	
	private static final String emptyXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>";

    @Test
    public void shouldIterateOverElementsCorrectly() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        List<Element> elementList = new ArrayList<>();
        while (xmlSpliterator.hasNext()) {
            elementList.add(xmlSpliterator.next());
        }
        assertThat(elementList.size(), is(4));
    }
    
    @Test
    public void shouldPlayWellWithGuavaFunctors() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        //1. test counting an iterable
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size, is(4));

        //2. test building a list from an iterable, then extract element-text from the list
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        List<Element> matches = Lists.newArrayList((Iterator<Element>) xmlSpliterator);
        assertThat(matches.size(), is(4));
        List<String> valueList = matches.stream().map(new ElementTextExtractor()).collect(Collectors.toList());
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));

        //3. test extracting element-text from the iterable directly
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        valueList = toXmlContentList(xmlSpliterator);
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));
    }

    //Java8 functors seem to perform _less_ efficiently than the Guava ones. For this
    //test sample (fairly small), there is a 100ms penalty (i5 dual-core HT, 30ms on an i7 quad-core HT)
    //using Java-8 functors compared to Guava.
    //Need to do some more testing against larger data-sets, but it does seem like a better
    //idea to use Guava for these types of transformations
    @Test
    public void shouldPlayWellWithJava8Functors() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        //1. test counting an iterable
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        //equivalent to the Guava Iterators.size
        long size = sequential(xmlSpliterator).count();
        assertThat(size, is(4L));

        //2. test building a list from an iterable, then extracting element-text from the list
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        List<Element> matches = sequential(xmlSpliterator).collect(Collectors.toList());
        assertThat(matches.size(), is(4));
        final List<String> valueList = matches.stream().map(new ElementTextExtractor()).collect(Collectors.toList());
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));

        //3. test iterating and extracting text from the iterable directly
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        valueList.clear();
        xmlSpliterator.forEach(e -> valueList.add(e.getTextTrim()));
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));
    }

    @Test
    public void shouldHandleEmptyXmlDoc() throws Exception {
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(new StringReader(emptyXmlDoc)), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size, is(0));
    }

    private List<String> toXmlContentList(XmlSpliterator xmlSpliterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(xmlSpliterator.iterator(), Spliterator.ORDERED), false)
            .map(input -> new ElementTextExtractor().apply(input)).collect(Collectors.toList());
    }

    private static final class ElementTextExtractor implements Function<Element, String>, java.util.function.Function<Element, String> {
        @Override
        public String apply(Element input) {
            return input.getTextTrim();
        }
    }
}
