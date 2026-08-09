package org.g5.util.stream.xml;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.g5.util.Streams.sequential;

/**
 * 
 * Source code licensed under the GNU GPL v3.0 or later.
 *
 */
class XmlSpliteratorTest {
	
	private static final String emptyXmlDoc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>";

    @Test
    void shouldIterateOverElementsCorrectly() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        List<Element> elementList = new ArrayList<>();
        while (xmlSpliterator.hasNext()) {
            elementList.add(xmlSpliterator.next());
        }
        assertThat(elementList.size()).isEqualTo(4);
    }
    
    @Test
    void shouldPlayWellWithGuavaFunctors() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        //1. test counting an iterable
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size).isEqualTo(4);

        //2. test building a list from an iterable, then extract element-text from the list
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        List<Element> matches = Lists.newArrayList((Iterator<Element>) xmlSpliterator);
        assertThat(matches.size()).isEqualTo(4);
        List<String> valueList = matches.stream().map(new ElementTextExtractor()).collect(Collectors.toList());
        assertThat(valueList).contains("abc", "def", "ghi", "klm");

        //3. test extracting element-text from the iterable directly
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        valueList = toXmlContentList(xmlSpliterator);
        assertThat(valueList).contains("abc", "def", "ghi", "klm");
    }

    //Java8 functors seem to perform _less_ efficiently than the Guava ones. For this
    //test sample (fairly small), there is a 100ms penalty (i5 dual-core HT, 30ms on an i7 quad-core HT)
    //using Java-8 functors compared to Guava.
    //Need to do some more testing against larger data-sets, but it does seem like a better
    //idea to use Guava for these types of transformations
    @Test
    void shouldPlayWellWithJava8Functors() throws Exception {
        File sourceFile = new File(getClass().getResource("/xml-spliterator.xml").getFile());
        //1. test counting an iterable
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        //equivalent to the Guava Iterators.size
        long size = sequential(xmlSpliterator).count();
        assertThat(size).isEqualTo(4L);

        //2. test building a list from an iterable, then extracting element-text from the list
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        List<Element> matches = sequential(xmlSpliterator).collect(Collectors.toList());
        assertThat(matches.size()).isEqualTo(4);
        final List<String> valueList = matches.stream().map(new ElementTextExtractor()).collect(Collectors.toList());
        assertThat(valueList).contains("abc", "def", "ghi", "klm");

        //3. test iterating and extracting text from the iterable directly
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        valueList.clear();
        xmlSpliterator.forEach(e -> valueList.add(e.getTextTrim()));
        assertThat(valueList).contains("abc", "def", "ghi", "klm");
    }

    @Test
    void shouldHandleEmptyXmlDoc() throws Exception {
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(new StringReader(emptyXmlDoc)), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size).isEqualTo(0);
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
