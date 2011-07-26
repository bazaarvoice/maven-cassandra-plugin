package com.bazaarvoice.prr.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.validation.Validator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used with {@link Validator} class during 
 * xml data importing.
 *
 */
public class BatchedErrorHandler implements ErrorHandler {

    private List<String> _errors = new LinkedList<String>();
    private List<String> _warnings = new LinkedList<String>();

    /**
     * This method is invoked if a recoverable error occurs.
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        _errors.add(String.format("Error at line %d, column %d: %s", exception.getLineNumber(), exception.getColumnNumber(), exception));
    }

    /**
     * This method is invoked if a non-recoverable error occurs. 
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    /**
     * This method is invoked if  a warning occurs.
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        _warnings.add(String.format("Warning at line %d, column %d: %s", exception.getLineNumber(), exception.getColumnNumber(), exception));
    }

    public List<String> getErrors() {
        return _errors;
    }

    public List<String> getWarnings() {
        return _warnings;
    }
}
