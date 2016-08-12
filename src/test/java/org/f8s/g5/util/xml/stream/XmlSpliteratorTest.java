package org.f8s.g5.util.xml.stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import org.f8s.g5.util.xml.stream.XmlSpliterator;
import org.jdom2.Element;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

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
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size, is(4));
        
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        List<Element> matches = Lists.newArrayList((Iterator<Element>)xmlSpliterator);
        assertThat(matches.size(), is(4));
        List<String> valueList = FluentIterable.from(matches).transform(new ElementTextExtractor()).toList();
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));
        
        xmlSpliterator = new XmlSpliterator(new StreamSource(sourceFile), "subchild");
        valueList = FluentIterable.from(xmlSpliterator).transform(new ElementTextExtractor()).toList();
        assertThat(valueList, containsInAnyOrder("abc", "def", "ghi", "klm"));
    }
    
    @Test
    public void shouldHandleEmptyXmlDoc() throws Exception {
        XmlSpliterator xmlSpliterator = new XmlSpliterator(new StreamSource(new StringReader(emptyXmlDoc)), "child");
        int size = Iterators.size(xmlSpliterator);
        assertThat(size, is(0));
    }


    private static final class ElementTextExtractor implements Function<Element, String> {
        @Override public String apply(Element input) {
            return input.getTextTrim();
        }
    }
}
