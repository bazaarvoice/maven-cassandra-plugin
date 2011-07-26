package com.bazaarvoice.core.jms.service;

import com.bazaarvoice.core.util.Locale;
import com.bazaarvoice.core.util.LocaleUtils;
import com.bazaarvoice.core.jms.util.BrokerAdapter;
import org.springframework.remoting.support.RemoteInvocationUtils;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDecoder {
    public static final String FIELD_MESSAGE_SIGNATURE = MessageEncoder.FIELD_MESSAGE_SIGNATURE;
    protected final BrokerAdapter _brokerAdapter;
    protected final MapMessage _mapMessage;

    public MessageDecoder(BrokerAdapter brokerAdapter, MapMessage mapMessage) {
        _brokerAdapter = brokerAdapter;
        _mapMessage = mapMessage;
    }

    // Message Header Property Getters

    /**
     * @see MessageEncoder#setMessageSignature(String)
     */
    public String getMessageSignature()
            throws JMSException {
        return getString(FIELD_MESSAGE_SIGNATURE);
    }

    public boolean getBooleanProperty(String name) throws JMSException {
        return _mapMessage.getBooleanProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
    }

    public int getIntProperty(String name) throws JMSException {
        return _mapMessage.getIntProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
    }

    public long getLongProperty(String name) throws JMSException {
        return _mapMessage.getLongProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
    }

    public String getStringProperty(String name) throws JMSException {
        return _mapMessage.getStringProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
    }

    public URI getUriProperty(String name) throws JMSException {
        String raw = getStringProperty(name);
        return (raw != null) ? URI.create(raw) : null;
    }

    public Object getObjectProperty(String name) throws JMSException {
        return _mapMessage.getObjectProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
    }

    public Date getDateProperty(String name) throws JMSException {
        Long millis = (Long) getObjectProperty(name);
        return (millis != null) ? new Date(millis) : null;
    }

    public <E extends Enum<E>> E getEnumProperty(String name, Class<E> enumClass) throws JMSException {
        String string = _mapMessage.getStringProperty(_brokerAdapter.sanitizeMessagePropertyName(name));
        return (string != null) ? Enum.valueOf(enumClass, string) : null;
    }

    public Integer decodeCollectionSizeProperty(String prefix) throws JMSException {
        return getIntProperty(prefix + "Count");
    }

    public <C extends Collection<URI>> C decodeURIsProperty(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSizeProperty(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            collection.add(getUriProperty(prefix + i));
        }
        return collection;
    }

    // MapMessage Getters

    public boolean itemExists(String name) throws JMSException {
        return _mapMessage.itemExists(name);
    }

    public boolean getBoolean(String name) throws JMSException {
        return _mapMessage.getBoolean(name);
    }

    public Boolean getBooleanObject(String name) throws JMSException {
        return (itemExists(name)) ? getBoolean(name) : null;
    }

    public int getInt(String name) throws JMSException {
        return _mapMessage.getInt(name);
    }

    public long getLong(String name) throws JMSException {
        return _mapMessage.getLong(name);
    }

    public double getDouble(String name) throws JMSException {
        return _mapMessage.getDouble(name);
    }

    public String getString(String name) throws JMSException {
        return _mapMessage.getString(name);
    }

    public byte[] getBytes(String name) throws JMSException {
        return _mapMessage.getBytes(name);
    }

    public Object getObject(String name) throws JMSException {
        return _mapMessage.getObject(name);
    }

    public Date getDate(String name) throws JMSException {
        Long millis = (Long) _mapMessage.getObject(name);
        return (millis != null) ? new Date(millis) : null;
    }

    public Locale getLocale(String name) throws JMSException {
        return LocaleUtils.safeToLocale(_mapMessage.getString(name));
    }

    public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass) throws JMSException {
        String string = _mapMessage.getString(name);
        return (string != null) ? Enum.valueOf(enumClass, string) : null;
    }

    public boolean hasThrowable() throws JMSException {
        return getBooleanProperty("BVException");
    }

    public RemoteMessageException decodeThrowable() throws JMSException {
        if (!hasThrowable()) {
            throw new IllegalArgumentException("JMS Message does not contain an encoded exception: " + _mapMessage);
        }

        // reconstruct the chain of exceptions starting with the inner-most
        String exceptionPrefix = "Exception_";
        int numExceptions = decodeCollectionSize(exceptionPrefix);
        RemoteMessageException t = null;
        for (int i = numExceptions - 1; i >= 0; i--) {

            String className = getString(exceptionPrefix + i + ".ClassName");
            String message = getString(exceptionPrefix + i + ".Message");

            String stackFramePrefix = exceptionPrefix + i + ".StackFrame_";
            int size = decodeCollectionSize(stackFramePrefix);
            StackTraceElement[] stackTraceElements = new StackTraceElement[size];
            for (int j = 0; j < size; j++) {
                String declaringClass = getString(stackFramePrefix + j + ".DeclaringClass");
                String methodName = getString(stackFramePrefix + j + ".MethodName");
                String fileName = getString(stackFramePrefix + j + ".FileName");
                int lineNumber = getInt(stackFramePrefix + j + ".LineNumber");
                stackTraceElements[j] = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
            }

            t = new RemoteMessageException(className, message, stackTraceElements, t);
        }

        // add the client-side stack trace so the server-side and client-side stack traces display together
        RemoteInvocationUtils.fillInClientStackTraceIfPossible(t);

        return t;
    }

    // Collection Decoder methods

    public Integer decodeCollectionSize(String prefix) throws JMSException {
        return (Integer) _mapMessage.getObject(prefix + "Count");
    }

    public <C extends Collection<String>> C decodeStrings(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            collection.add(getString(prefix + i));
        }
        return collection;
    }

    public <C extends Collection<Long>> C decodeLongs(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            collection.add((Long) getObject(prefix + i)); // null-safe
        }
        return collection;
    }

    public <C extends Collection<Boolean>> C decodeBooleans(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            collection.add((Boolean) getObject(prefix + i)); // null-safe
        }
        return collection;
    }

    public <C extends Collection<Date>> C decodeDates(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            Object longValue = getObject(prefix + i);
            collection.add(longValue != null ? new Date((Long) longValue) : null); // null-safe
        }
        return collection;
    }

    public <C extends Collection<Locale>> C decodeLocales(String prefix, C collection) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            collection.add(getLocale(prefix + i));
        }
        return collection;
    }

    public <E extends Enum<E>> EnumSet<E> decodeEnums(String prefix, Class<E> enumClass) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        EnumSet<E> collection = EnumSet.noneOf(enumClass);
        for (int i = 0; i < size; i++) {
            collection.add(getEnum(prefix + i, enumClass));
        }
        return collection;
    }

    public <E extends Enum<E>> List<E> decodeEnumList(String prefix, Class<E> enumClass) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        List<E> enumList = new ArrayList<E>();
        for (int i = 0; i < size; i++) {
            enumList.add(getEnum(prefix + i, enumClass));
        }
        return enumList;
    }

    public <K, V> Map<K, V> decodePrimitivesMap(String prefix) throws JMSException {
        return decodePrimitivesMap(prefix, "Key");
    }

    @SuppressWarnings ({"unchecked"})
    public <K, V> Map<K, V> decodePrimitivesMap(String prefix, String keyName) throws JMSException {
        Integer size = decodeCollectionSize(prefix);
        if (size == null) {
            return null;
        }
        Map<K, V> map = new HashMap<K, V>();
        for (int index = 0; index < size; index++) {
            K key = (K) getObject(prefix + index + "." + keyName);
            V value = (V) getObject(prefix + index + ".Value");
            map.put(key, value);
        }
        return map;
    }
}
