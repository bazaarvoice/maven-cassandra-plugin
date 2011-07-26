package com.bazaarvoice.core.jms.service;

import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.core.jms.util.BrokerAdapter;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Session;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageEncoder {
    public static final String FIELD_MESSAGE_SIGNATURE = "MessageSignature";
    protected final BrokerAdapter _brokerAdapter;
    protected final MapMessage _mapMessage;

    public MessageEncoder(BrokerAdapter brokerAdapter, Session session)
            throws JMSException {
        _mapMessage = session.createMapMessage();
        _brokerAdapter = brokerAdapter;
    }

    /**
     * the 'signature' of the message is the unique ID of the message format,
     * which may change between versions (ie., to allow one cluster to ignore messages from clusters of
     * other versions).
     */
    public void setMessageSignature(String value)
            throws JMSException {
        setString(FIELD_MESSAGE_SIGNATURE, value);
    }

    public MapMessage toMessage() {
        return _mapMessage;
    }
    
    // Message Header Property Setters

    public void setBooleanProperty(String name, boolean value) throws JMSException {
        _mapMessage.setBooleanProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value);
    }

    public void setIntProperty(String name, int value) throws JMSException {
        _mapMessage.setIntProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value);
    }

    public void setLongProperty(String name, long value) throws JMSException {
        _mapMessage.setLongProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value);
    }

    public void setStringProperty(String name, String value) throws JMSException {
        if (value != null) {
            _mapMessage.setStringProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value);
        }
    }

    public void setUriProperty(String name, URI value) throws JMSException {
        if (value != null) {
            _mapMessage.setStringProperty(name, value.toASCIIString());
        }
    }

    public void setObjectProperty(String name, Object value) throws JMSException {
        if (value != null) {
            _mapMessage.setObjectProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value);
        }
    }

    public void setDateProperty(String name, Date date) throws JMSException {
        if (date != null) {
            setLongProperty(name, date.getTime());
        }
    }

    public void setEnumProperty(String name, Enum value) throws JMSException {
        if (value != null) {
            _mapMessage.setStringProperty(_brokerAdapter.sanitizeMessagePropertyName(name), value.name());
        }
    }

    public void encodeCollectionSizeProperty(String prefix, int size) throws JMSException {
        setIntProperty(prefix + "Count", size);
    }

    public void encodeURIsProperty(String prefix, Collection<URI> uris) throws JMSException {
        if (uris != null) {
            int index = 0;
            for (URI uri : uris) {
                setUriProperty(prefix + index, uri);
                index++;
            }
            encodeCollectionSizeProperty(prefix, uris.size());
        }
    }

    // MapMessage Setters

    public void setBoolean(String name, boolean value) throws JMSException {
        _mapMessage.setBoolean(name, value);
    }

    public void setInt(String name, int value) throws JMSException {
        _mapMessage.setInt(name, value);
    }

    public void setLong(String name, long value) throws JMSException {
        _mapMessage.setLong(name, value);
    }

    public void setDouble(String name, double value) throws JMSException {
        _mapMessage.setDouble(name, value);
    }

    public void setString(String name, String value) throws JMSException {
        if (value != null) {
            _mapMessage.setString(name, value);
        }
    }

    public void setBytes(String name, byte[] value) throws JMSException {
        if (value != null) {
            _mapMessage.setBytes(name, value);
        }
    }

    public void setObject(String name, Object value) throws JMSException {
        if (value != null) {
            _mapMessage.setObject(name, value);
        }
    }

    public void setDate(String name, Date date) throws JMSException {
        if (date != null) {
            _mapMessage.setLong(name, date.getTime());
        }
    }

    public void setLocale(String name, Locale locale) throws JMSException {
        if (locale != null) {
            _mapMessage.setString(name, locale.toString());
        }
    }

    public void setEnum(String name, Enum value) throws JMSException {
        if (value != null) {
            _mapMessage.setString(name, value.name());
        }
    }

    public void encodeThrowable(Throwable t) throws JMSException {
        // the "BVException" header is reserved for identifying messages containing exceptions
        setBooleanProperty("BVException", true);

        String exceptionPrefix = "Exception_";
        int exceptionIndex = 0;
        Set<Throwable> visitedExceptions = new HashSet<Throwable>();  // detect ex.getCause() == ex, being paranoid (see #7070)
        while (t != null && visitedExceptions.add(t)) {
            setString(exceptionPrefix + exceptionIndex + ".ClassName", t.getClass().getName());
            setString(exceptionPrefix + exceptionIndex + ".Message", t.getLocalizedMessage());

            String stackFramePrefix = exceptionPrefix + exceptionIndex + ".StackFrame_";
            StackTraceElement[] stackTraceElements = t.getStackTrace();
            for (int i = 0; i < stackTraceElements.length; i++) {
                StackTraceElement stackTraceElement = stackTraceElements[i];
                setString(stackFramePrefix + i + ".DeclaringClass", stackTraceElement.getClassName());
                setString(stackFramePrefix + i + ".MethodName", stackTraceElement.getMethodName());
                setString(stackFramePrefix + i + ".FileName", stackTraceElement.getFileName());
                setInt(stackFramePrefix + i + ".LineNumber", stackTraceElement.getLineNumber());
            }
            encodeCollectionSize(stackFramePrefix, stackTraceElements.length);

            t = t.getCause();
            exceptionIndex++;
        }
        encodeCollectionSize(exceptionPrefix, exceptionIndex);
    }

    // Collection Encoder methods

    public void encodeCollectionSize(String prefix, int size) throws JMSException {
        _mapMessage.setInt(prefix + "Count", size);
    }

    public void encodeStrings(String prefix, Collection<String> strings) throws JMSException {
        if (strings != null) {
            int index = 0;
            for (String string : strings) {
                setString(prefix + index, string);
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    public void encodeLongs(String prefix, Collection<Long> longs) throws JMSException {
        if (longs != null) {
            int index = 0;
            for (Long longVal : longs) {
                setObject(prefix + index, longVal); // null-safe
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    public void encodeBooleans(String prefix, Collection<Boolean> booleans) throws JMSException {
        if (booleans != null) {
            int index = 0;
            for (Boolean boolVal : booleans) {
                setObject(prefix + index, boolVal); // null-safe
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    public void encodeDates(String prefix, Collection<Date> dates) throws JMSException {
        if (dates != null) {
            int index = 0;
            for (Date dateVal : dates) {
                setObject(prefix + index, dateVal != null ? dateVal.getTime() : null); // null-safe
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    public void encodeLocales(String prefix, Collection<Locale> locales) throws JMSException {
        if (locales != null) {
            int i = 0;
            for (Locale locale : locales) {
                setLocale(prefix + i, locale);
                i++;
            }
            encodeCollectionSize(prefix, i);
        }
    }

    public <E extends Enum<E>> void encodeEnums(String prefix, Collection<E> values) throws JMSException {
        if (values != null) {
            int index = 0;
            for (E value : values) {
                setEnum(prefix + index, value);
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    public <E extends Enum<E>> void encodeEnumList(String prefix, List<E> values) throws JMSException {
        if (values != null) {
            int index = 0;
            for (E value : values) {
                setEnum(prefix + index, value);
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }

    /**
     * Encodes a map of key/value pairs, where the type of each key and value is either a
     * basic Java primitive type (Boolean, Integer, Float, etc.) or type String.
     */
    public void encodePrimitivesMap(String prefix, Map<?, ?> stringsMap) throws JMSException {
        encodePrimitivesMap(prefix, "Key", stringsMap);
    }

    /**
     * Encodes a map of key/value pairs, where the type of each key and value is either a
     * basic Java primitive type (Boolean, Integer, Float, etc.) or type String.
     */
    public void encodePrimitivesMap(String prefix, String keyName, Map<?, ?> stringsMap) throws JMSException {
        if (stringsMap != null) {
            int index = 0;
            for (Map.Entry<?, ?> entry : stringsMap.entrySet()) {
                setObject(prefix + index + "." + keyName, entry.getKey());
                setObject(prefix + index + ".Value", entry.getValue());
                index++;
            }
            encodeCollectionSize(prefix, index);
        }
    }
}
