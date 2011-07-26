package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.util.multimap.HashMapArrayListMultiMap;
import com.bazaarvoice.cca.util.multimap.ListMultiMap;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlSaxHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class splits an XML document up into chunks based on a particular element.  This is useful
 * for breaking up large documents into smaller chunks so that you can use DOM or XMLBeans based
 * processing.
 *
 * Note that the SAX parser must be namespace aware.
 *
 * Also note that XMLChunkHandler splits based on one target element name, not an XPath, so a schema that allows elements
 * of the same name to be nested within each other will not function correctly.  XMLChunkHandler will start splitting
 * on each matched element and will end up returning the innermost element chunk multiple times when backing out of
 * the tree as it finds matching closing tags. 
 *
 * This was originally intended for use with the syndication feed XSD so this has not been tested
 * under all conditions.  User beware.
 *
 * @see XMLChunkHandler
 */
public class XMLSplitter extends DefaultHandler {
    private final XmlOptions _xmlOptions;
    private final ListMultiMap<String, HandlerMapping> _targetElementHandlers = new HashMapArrayListMultiMap<String, HandlerMapping>();
    private final List<String> _elementNameStack = new ArrayList<String>();
    private final List<Chunk> _chunkStack = new ArrayList<Chunk>();
    private Chunk _chunk;
    private Locator _locator;

    public XMLSplitter(XmlOptions xmlOptions) {
        _xmlOptions = xmlOptions;
    }

    public XMLSplitter(String targetElement, XMLChunkHandler chunkHandler, XmlOptions xmlOptions) {
        this(xmlOptions);
        addHandlerMapping(targetElement, chunkHandler);
    }

    public void addHandlerMapping(String elementPath, XMLChunkHandler chunkHandler) {
        HandlerMapping mapping = new HandlerMapping(elementPath, chunkHandler);
        _targetElementHandlers.add(mapping.getMostSpecificElement(), mapping);
    }

    private HandlerMapping getMatchingHandlerMapping(List<String> elementNameStack) {
        // o(1) hash lookup on the leaf element name then simple o(n) loop through all the mappings with the same leaf element name
        String mostSpecificElement = elementNameStack.get(elementNameStack.size() - 1);
        for (HandlerMapping mapping : _targetElementHandlers.getCollection(mostSpecificElement)) {
            if (mapping.matches(elementNameStack)) {
                return mapping;
            }
        }
        return null;
    }

    public void startDocument() throws SAXException {
        _elementNameStack.clear();
        _elementNameStack.add(""); // placeholder for the document root
        _chunkStack.clear();
        _chunk = null;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        _elementNameStack.add(localName); // push the element on top of the element stack

        // check to see if we need to start a new chunk / chunk handler
        HandlerMapping mapping = getMatchingHandlerMapping(_elementNameStack);
        if (mapping != null) {
            XMLChunkHandler chunkHandler = mapping.getChunkHandler(); // might return null to skip this element
            _chunkStack.add(_chunk); // save the current chunk and start a new one
            _chunk = new Chunk(chunkHandler, _locator.getLineNumber(), _locator.getColumnNumber());
        }

        if (_chunk != null) {
            _chunk.startElement(uri, localName, qName, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (_chunk != null) {
            _chunk.characters(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (_chunk != null) {
            if (_chunk.endElement(uri, localName, qName)) {
                _chunk.finish();
                _chunk = _chunkStack.remove(_chunkStack.size() - 1); // pop the old chunk, might be null
            }
        }

        _elementNameStack.remove(_elementNameStack.size() - 1); // pop the top of the element stack
    }

    public void setDocumentLocator(Locator locator) {
        _locator = locator;
    }

    /**
     * Maps a XML subpath like 'Feed/Categories/Category' to a XMLChunkHandler.
     */
    private static class HandlerMapping {
        private final String[] _elementSubpath;
        private final XMLChunkHandler _chunkHandler;  // might be null

        private HandlerMapping(String elementSubpath, XMLChunkHandler chunkHandler) {
            _elementSubpath = elementSubpath.split("/");  // leading '/' splits to a leading "" element that matches the document root 
            _chunkHandler = chunkHandler;
        }

        private String getMostSpecificElement() {
            return _elementSubpath[_elementSubpath.length - 1];
        }

        private boolean matches(List<String> elementStack) {
            if (_elementSubpath.length > elementStack.size()) {
                return false;
            }
            for (int i = _elementSubpath.length - 1, j = elementStack.size() - 1; i >= 0; i--, j--) {
                if (!_elementSubpath[i].equals(elementStack.get(j))) {
                    return false;
                }
            }
            return true;
        }

        private XMLChunkHandler getChunkHandler() {
            return _chunkHandler;
        }
    }

    private class Chunk {
        private final XMLChunkHandler _chunkHandler;
        private final int _lineNumber;
        private final int _column;
        private final XmlSaxHandler _saxHandler;
        private final ContentHandler _contentHandler;
        private int _elementDepth;

        public Chunk(XMLChunkHandler chunkHandler, int lineNumber, int column) throws SAXException {
            _chunkHandler = chunkHandler;
            _lineNumber = lineNumber;
            _column = column;

            if (chunkHandler != null) {
                _saxHandler = XmlObject.Factory.newXmlSaxHandler(_xmlOptions);
                _contentHandler = _saxHandler.getContentHandler();
                _contentHandler.setDocumentLocator(_locator);
                _contentHandler.startDocument();
            } else {
                // a null chunk handler causes the content in the chunk to be ignored 
                _saxHandler = null;
                _contentHandler = null;
            }
        }

        public void finish() throws SAXException {
            if (_contentHandler != null) {
                _contentHandler.endDocument();
                // get the parsed XmlBeans XmlObject and cast it to the expected generated interface
                XmlObject xmlObject;
                try {
                    xmlObject = _saxHandler.getObject();
                } catch (XmlException e) {
                    throw new SAXException(e);
                }
                if (!_chunkHandler.getChunkType().isInstance(xmlObject)) {
                    throw new ClassCastException("Unable to cast XmlObject " + xmlObject.getClass().getName() + " to " + _chunkHandler.getChunkType().getName());
                }
                //noinspection unchecked
                _chunkHandler.processChunk(_lineNumber, _column, xmlObject);
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (_contentHandler != null) {
                _contentHandler.startElement(uri, localName, qName, attributes);
            }
            _elementDepth++;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (_contentHandler != null) {
                _contentHandler.characters(ch, start, length);
            }
        }

        public boolean endElement(String uri, String localName, String qName) throws SAXException {
            if (_contentHandler != null) {
                _contentHandler.endElement(uri, localName, qName);
            }
            return --_elementDepth == 0;  // return true if we're ending the top-level element in this chunk
        }
    }
}