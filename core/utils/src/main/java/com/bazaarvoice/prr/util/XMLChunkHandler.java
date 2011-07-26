package com.bazaarvoice.prr.util;

import org.apache.xmlbeans.XmlObject;

/**
 * An interface for processing chunks of xml from XMLSplitter.
 *
 * @see XMLSplitter
 */
public interface XMLChunkHandler<T extends XmlObject> {

    Class<T> getChunkType();

    /**
     * This method is called by XMLSplitter with a chunk of extracted XML.  The root
     * element will have an xmlns defined.  There will be no document tags.
     *
     * @param line The line number where the chunk started in the original document
     * @param column The column where the chunk started in the original document
     * @param xmlObject A parsed XmlObject
     */
    void processChunk(int line, int column, T xmlObject);
}
