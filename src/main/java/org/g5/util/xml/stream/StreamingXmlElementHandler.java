package org.g5.util.xml.stream;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;

public abstract class StreamingXmlElementHandler {
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    
    public String elementAsString(XMLStreamReader reader) throws XMLStreamException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(buffer);
        final String namespaceURI = reader.getNamespaceURI();
        if (StringUtils.isNotEmpty(namespaceURI)) {
            writer.writeStartElement(reader.getPrefix(), reader.getLocalName(), namespaceURI);
        } else {
            writer.writeStartElement(reader.getLocalName());
        }
        // Handle attributes
        for (int i=0, len=reader.getAttributeCount(); i<len; i++) {
            final String attributeNamespace = reader.getAttributeNamespace(i);
            if (StringUtils.isNotEmpty(attributeNamespace)) {
                writer.writeAttribute(reader.getAttributePrefix(i), attributeNamespace, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
        // Handle Namespaces
        for (int i = 0, len = reader.getNamespaceCount(); i < len; i++) {
            writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
        }
        writer.flush();
        String elementString = new String(buffer.toByteArray());
        if (!elementString.endsWith(">")) {
            elementString = elementString + ">";
        }
        return elementString;
    }
    
    public abstract void handleStartElementEvent(String elementAsString);
    public abstract void handleEndElementEvent(String elementName);
}
