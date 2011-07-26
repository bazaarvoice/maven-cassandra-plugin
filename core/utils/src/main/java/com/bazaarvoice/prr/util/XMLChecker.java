package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.util.IOUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is used to check if input stream is XML.
 *
 * @author ilyasch
 */

public abstract class XMLChecker {

    private static final int ENCODING_LENGTH = 4;

    private static final int XML_HEADER_LENGTH = 16;

    private static final String[] XML_HEADERS = {"<?xml", "<Feed"};

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    // Names of encodings
    public static final String UTF_8 = "UTF-8";

    public static final String UTF_16BE = "UTF-16BE";

    public static final String UTF_16LE = "UTF-16LE";

    public static final String CP037 = "CP037";
    // //////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Gets encoding of stream <code>input</code>
     *
     * @param input input stream.
     * @return encoding the name of encoding
     * @throws IOException If an I/O error occurs
     */
    private static String getEncoding(final InputStream input)
            throws IOException {
        byte[] buffer = new byte[4];
        IOUtils.readFully(input, buffer);
        int j = buffer[0] & 255;
        int k = buffer[1] & 255;
        // This algorithm was borrowed from Apache Xerces implementation.
        // The support of ISO_10646_UCS_4 was left out because this encoding
        // isn't supported by Java 1.5.
        // By default used UTF-8
        if (j == 254 && k == 255) {
            return UTF_16BE;
        }
        if (j == 255 && k == 254) {
            return UTF_16LE;
        }
        int l = buffer[2] & 255;
        if (j == 239 && k == 187 && l == 191) {
            return UTF_8;
        }
        int i1 = buffer[3] & 255;
        if (j == 0 && k == 60 && l == 0 && i1 == 63) {
            return UTF_16BE;
        }
        if (j == 60 && k == 0 && l == 63 && i1 == 0) {
            return UTF_16LE;
        }
        if (j == 76 && k == 111 && l == 167 && i1 == 148) {
            return CP037;
        }
        return UTF_8;
    }

    /**
     * Checks if stream is XML.
     *
     * @param input input stream
     * @return <code>true</code> if input stream is XML , <code>false</code>
     *         otherwise.
     * @throws IOException If an I/O error occurs
     */
    public static boolean isXML(InputStream input)
            throws IOException {
        if (!input.markSupported()) {
            throw new IllegalArgumentException("InputStream must support the mark() method: " + input.getClass().getName());
        }

        // auto detect the character encoding by looking at the first few bytes of the stream
        input.mark(ENCODING_LENGTH);
        String charsetName;
        try {
            charsetName = getEncoding(input);
        } finally {
            input.reset();
        }

        // read the first n chars, reading * 4 bytes to allow for multi-byte characters.
        input.mark(XML_HEADER_LENGTH * 4);
        try {
            byte[] buf = new byte[XML_HEADER_LENGTH * 4];
            int count = IOUtils.readFully(input, buf);
            if (count == -1) {
                return false;
            }
            String actualHeader = new String(buf, 0, count, charsetName);
            for (String xmlHeader : XML_HEADERS) {
                if (actualHeader.contains(xmlHeader)) {
                    return true;
                }
            }
            return false;
        } finally {
            input.reset();
        }
    }

    /** Returns the xmlns of the root element of the specified XML file. */
    public static String getDocumentElementNamespaceURI(InputStream in) throws IOException, XMLStreamException {
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(in);
        try {
            while (!xmlReader.isStartElement()) {
                xmlReader.next();
            }
            return xmlReader.getNamespaceURI();
        } finally {
            xmlReader.close();
        }
    }

    /** Returns the xmlns of the root element of the specified XML file. */
    public static String getDocumentElementNamespaceURI(File file) throws IOException, XMLStreamException {
        InputStream in = new FileInputStream(file);
        try {
            return XMLChecker.getDocumentElementNamespaceURI(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /** Returns the specified attribute of the root element of the specified XML file. */
    public static String getDocumentElementAttribute(InputStream in, String attributeName) throws XMLStreamException {
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(in);
        try {
            while (!xmlReader.isStartElement()) {
                xmlReader.next();
            }
            return xmlReader.getAttributeValue(null, attributeName);
        } finally {
            xmlReader.close();
        }
    }

}
