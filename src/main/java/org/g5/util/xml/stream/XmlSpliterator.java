package org.g5.util.xml.stream;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.g5.util.GZipper;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.StAXStreamBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * I use StAX to process large XML documents and split them up into chunks based on the specified element name.
 * I return a JDOM2 {@link Element element}, which provides an easy API for manipulating sub-sections of the large document.
 * I can process an XML of arbitrary depth, and extract sequences of names elements. These elements will be lazily offered
 * for consumption through either my {@link Iterator iterator} or {@link Iterable iterable} interfaces.
 * This makes processing large XML documents, for the purpose of extracting element sequences, very memory efficient.
 * 
 * Source code licensed under the GNU GPL v3.0 or later.
 * 
 * @author gerard.fernandes@gmail.com
 */
public class XmlSpliterator implements Iterator<Element>, Iterable<Element> {

    private static final Logger log = LoggerFactory.getLogger(XmlSpliterator.class);
    
    private final XMLStreamReader xmlStreamReader;
    private final String splitOnElementName;
    //We need this to track whole element sections. When an element is fully processed - i.e. it's end-element tag is reached - we
    //pop it off the stack. This also tells us when we've hit the effective end of the document - i.e. we've hit the root element end-tag.
    //At this point, the stack will be empty.
    private final Stack<String> elementStack = new Stack<>();
    private final StreamingXmlElementHandler skippedElementConsumer;
    //temporal cached element - the hasNext() call must find a matching element to be able to handle empty documents
    private Optional<Element> selectedElement = Optional.empty();
    
    public XmlSpliterator(Source xmlSource, String splitOnElementName) throws XMLStreamException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(xmlSource), splitOnElementName, null);
    }
    
    public XmlSpliterator(Source xmlSource, String splitOnElementName, StreamingXmlElementHandler skippedElementConsumer) throws XMLStreamException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(xmlSource), splitOnElementName, skippedElementConsumer);
    }
    
    public XmlSpliterator(XMLStreamReader xmlStreamReader, String splitOnElementName) throws XMLStreamException {
        this(xmlStreamReader, splitOnElementName, null);
    }
    public XmlSpliterator(XMLStreamReader xmlStreamReader, String splitOnElementName, StreamingXmlElementHandler skippedElementConsumer) throws XMLStreamException {
        this.xmlStreamReader = xmlStreamReader;
        Validate.isTrue(isNotEmpty(splitOnElementName), "No split element name specified! Can not continue!");
        this.splitOnElementName = splitOnElementName;
        this.skippedElementConsumer = Optional.ofNullable(skippedElementConsumer).orElse(new DefaultSkippedElementConsumer());
    }
    
    @Override
    public boolean hasNext() {
        if ( ! selectedElement.isPresent()){
            selectedElement = Optional.ofNullable(findNextElement());
        }
        return selectedElement.isPresent();
    }
    
    @Override
    public Element next() {
        if (hasNext()) {
            Element copy = selectedElement.get().clone();
            selectedElement = Optional.empty();
            return copy;
        }
        throw new IllegalStateException("No more elements available! Empty document, or end of stream reached.");
    }
    
    private Element findNextElement() {
        try {
            for (; xmlStreamReader.hasNext(); xmlStreamReader.next()) {
                if (xmlStreamReader.isStartElement()){
                    elementStack.push(xmlStreamReader.getLocalName());
                    log.debug("Pushed element["+xmlStreamReader.getLocalName()+"]");
                    if (xmlStreamReader.getLocalName().equals(splitOnElementName)) {//selected element - extract and return
                        log.debug("Processed and popping element["+xmlStreamReader.getLocalName()+"]");
                        Content fragment = new StAXStreamBuilder().fragment(xmlStreamReader);
                        //we're done processing this element, and we won't hit it's end tag as thats been consumed by the fragment above, so pop it
                        elementStack.pop();
                        return (Element) fragment;
                    } else {//rejected element - pass to the skipped element consumer
                        skippedElementConsumer.handleStartElementEvent(skippedElementConsumer.elementAsString(xmlStreamReader));
                    }
                } else if (xmlStreamReader.isEndElement()) {
                    unwindStackOnEndElements();
                }
            }
        } catch (XMLStreamException | JDOMException e) {
            log.error("Failed to retrieve next element from stream.", e);
        }
        log.info("No more elements - end of stream reached!");
        return null;
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Iterator<Element> iterator() {
        return this;
    }
    
    private void unwindStackOnEndElements() throws XMLStreamException {
        for (;xmlStreamReader.isEndElement() || xmlStreamReader.isCharacters(); xmlStreamReader.next()) {
            if (xmlStreamReader.isCharacters()) continue; 
            if (xmlStreamReader.getLocalName().equals(elementStack.peek())) {
                elementStack.pop();
                skippedElementConsumer.handleEndElementEvent(xmlStreamReader.getLocalName());
                log.debug("Popped element["+xmlStreamReader.getLocalName()+"]");
                break;
            }
        }
    }
    
    private static final class DefaultSkippedElementConsumer extends StreamingXmlElementHandler {

        @Override
        public void handleStartElementEvent(String elementAsString) {
            return;
        }

        @Override
        public void handleEndElementEvent(String elementName) {
            return;
        }
    }
    
    //this is one use-case for this XMLSpliterator - here, a very large 600+ MB XML trade file is processed to extract the uncompressed IDM FVar
    //from each trade, and write it out to a file named after the trade contract-id
    public static void main(String[] args) throws Exception {
        XmlSpliterator spliterator = new XmlSpliterator(new StreamSource(new File("/dev/projects/temp/getjack1.xml")), "OTCTrade");
        while (spliterator.hasNext()) {
            Element e = spliterator.next();
            String contractId = e.getChildText("contractId");
            log.info("Writing out IDM for contract-id["+contractId+"]");
            File outputFile = new File("/dev/projects/temp/jack", contractId+".idm");
            IOUtils.write(GZipper.decompress(Base64.getDecoder().decode(e.getChildText("compressedIdmFVar"))), new FileOutputStream(outputFile));
        }
    }

}
