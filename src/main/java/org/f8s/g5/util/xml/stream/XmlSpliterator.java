package org.f8s.g5.util.xml.stream;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.f8s.g5.util.GZipper;
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
    //This flag is mostly there to indicate processing has started - which is the point when we begin to care about the state of the elementStack.
    private boolean processing = false;
    
    public XmlSpliterator(Source xmlSource, String splitOnElementName)
            throws XMLStreamException, FactoryConfigurationError, TransformerConfigurationException, TransformerFactoryConfigurationError {
        xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlSource);
        Validate.isTrue(isNotEmpty(splitOnElementName), "No split element name specified! Can not continue!");
        this.splitOnElementName = splitOnElementName;
    }
    
    @Override
    public boolean hasNext() {
        try {
            return xmlStreamReader.hasNext() && ! reachedRootElementEndTag();
        } catch (XMLStreamException e) {
            return false;
        }
    }
    
    @Override
    public Element next() {
        if (!processing) {//indicate we're processing the document now...
            processing = true;
        }
        try {
            for (; xmlStreamReader.hasNext(); xmlStreamReader.next()) {
                if (xmlStreamReader.isStartElement()){
                    elementStack.push(xmlStreamReader.getLocalName());
                    log.debug("Pushed element["+xmlStreamReader.getLocalName()+"]");
                    if (xmlStreamReader.getLocalName().equals(splitOnElementName)) {
                        Content fragment = new StAXStreamBuilder().fragment(xmlStreamReader);
                        //we're done processing this element, and we won't hit it's end tag as thats been consumed by the fragment above, so pop it
                        elementStack.pop();
                        log.debug("Processed and popped element["+splitOnElementName+"]");
                        return (Element) fragment;
                    }
                }
            }
        } catch (XMLStreamException | JDOMException e) {
            log.error("Failed to retrieve next element from stream.", e);
        }
        throw new IllegalStateException("Got a 'null'! We should never come here, as this indicates a fault in the 'hasNext()' logic.");
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Iterator<Element> iterator() {
        return this;
    }
    
    private boolean reachedRootElementEndTag() throws XMLStreamException {
        //We must consume any remaining end-element events here, and pop these off our stack. This is so
        //that we can accurately report whether we have any more elements we can realistically process.
        //Towards the end of a document, we'll typically "unwind" elements (hit multiple end-element events) and eventually hit the end-tag.
        //We can not process these in the next() call as this will result in a null value being returned. This is why we
        //must consume these events in the hasNext() check.
    	unwindStackOnEndElements();
        return processing && elementStack.empty();
    }
    
    private void unwindStackOnEndElements() throws XMLStreamException {
        for (;xmlStreamReader.isEndElement() || xmlStreamReader.isCharacters(); xmlStreamReader.next()) {
            if (xmlStreamReader.isCharacters()) continue; 
            if (xmlStreamReader.getLocalName().equals(elementStack.peek())) {
                elementStack.pop();
                log.debug("Popping element["+xmlStreamReader.getLocalName()+"]");
            }
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
