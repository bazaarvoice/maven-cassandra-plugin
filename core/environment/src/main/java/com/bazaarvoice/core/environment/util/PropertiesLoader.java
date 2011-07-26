package com.bazaarvoice.core.environment.util;

import com.bazaarvoice.core.util.Assert;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an XML version of a .properties file format, extended with
 * conditional processing and string template expansion.
 */
public class PropertiesLoader {

    public static interface DecryptionAdapter {
        String decryptString(String key, String value) throws Exception;
    }

    /**
     * XML namespace of the elements in the XML file.  None for now.
     */
    private static final String NS = "";

    /**
     * Name of the DTD file referenced in the systemID of a <!DOCTYPE> declaration.
     */
    private static final String DTD_URI = "PropertiesLoader.dtd";

    /**
     * For JDK-compatibility, the URI of the JDK's properties file format.
     */
    private static final String JDK_DTD_URI =
            "http://java.sun.com/dtd/properties.dtd";

    /**
     * For JDK-compatibility, the JDK's properties file DTD.  Wish this was exposed by the JDK...
     */
    private static final String JDK_DTD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!ELEMENT properties ( comment?, entry* ) >" +
                    "<!ATTLIST properties version CDATA #FIXED \"1.0\">" +
                    "<!ELEMENT comment (#PCDATA) >" +
                    "<!ELEMENT entry (#PCDATA) >" +
                    "<!ATTLIST entry key CDATA #REQUIRED>";

    /**
     * The parsed abstract syntax tree corresponding to the XML structure of the properties.
     */
    private final ASTStatement _block;

    /**
     * An optional decryption adapter to handle loading encrypted values.
     * TODO: Refactor PropertiesLoader into separate parser and loader classes.
     */
    private static DecryptionAdapter _decryptionAdapter;

    private PropertiesLoader(ASTStatement block) {
        _block = block;
    }

    public static void setDecryptionAdapter(DecryptionAdapter decryptionAdapter) {
        _decryptionAdapter = decryptionAdapter;
    }

    /**
     * Populates a set of properties using the instructions in the Properties XML file.
     *
     * @param props the set of properties to update
     */
    public void load(final Properties props) {
        _block.execute(props);
    }

    /**
     * Manual testing method.
     */
    public static void main(String[] args) throws Exception {
        try {
            if (args.length == 0) {
                System.out.println("usage: " + PropertiesLoader.class.getName() + " <properties.xml> ...");
                System.exit(1);
            }
            Properties parent = System.getProperties();
            for (String fileName : args) {
                System.out.println(fileName + ":");
                Properties props = new Properties(parent);

                parse(new File(fileName)).load(props);

                print(props);
                parent = props;
            }
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    /**
     * Manual testing method.
     */
    public static void print(Properties props) {
        // by design this ignores inherited properties
        for (Map.Entry entry : new TreeMap<Object, Object>(props).entrySet()) {
            String propName = (String) entry.getKey();
            String propValue = String.valueOf(entry.getValue());
            if (propName.contains("password")) {
                propValue = propValue.replaceAll(".", "*");
            }
            System.out.println(" " + propName + " = " + propValue);
        }
    }

    /**
     * Concatenates a list of PropertiesLoader objects into a single ProeprtiesLoader.
     */
    public static PropertiesLoader concat(Iterable<PropertiesLoader> loaders) {
        ASTBlock block = new ASTBlock();
        for (PropertiesLoader loader : loaders) {
            block.add(loader._block);
        }
        return new PropertiesLoader(block);
    }

    /**
     * Parses an XML or .properties file and returns a PropertiesLoader with the contents.
     */
    public static PropertiesLoader parse(File file) throws XMLStreamException, IOException {
        PropertiesLoader loader;
        if (file.getName().endsWith(".xml")) {
            // extended properties XML file
            loader = PropertiesLoader.parseXML(new StreamSource(file));
        } else {
            // regular JDK .properties file
            FileInputStream in = new FileInputStream(file);
            loader = PropertiesLoader.parse(in);
            in.close();
        }
        return loader;
    }

    /**
     * Parses an standard properties file and returns a loader
     * that can update a Properties with the result.
     */
    public static PropertiesLoader parse(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);

        ASTBlock block = new ASTBlock();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            block.add(new ASTAssignment(new ASTLiteral(key), new ASTLiteral(value), false, false));
        }

        return new PropertiesLoader(block);
    }

    //
    // XML parsing methods.  see PropertiesLoader.dtd
    //

    /**
     * Parses an XML properties file and returns a loader
     * that can update a Properties with the result.
     */
    public static PropertiesLoader parseXML(Source xmlSource) throws XMLStreamException {
        ASTStatement block;

        XMLInputFactory factory = XMLInputFactory.newInstance();

        // turn on DTD validation and look for the DTD in the classpath, same location as this class
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.TRUE);
        factory.setXMLResolver(new XMLResolver() {
            public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) {
                if (DTD_URI.equals(systemID)) {
                    // DTD is in the classpath, same directory as this class
                    InputStream in = PropertiesLoader.class.getResourceAsStream(DTD_URI);
                    return new StreamSource(in, systemID);
                }
                if (JDK_DTD_URI.equals(systemID)) {
                    // for backwards compatibility with the JDK 1.5 Properties XML format...
                    return new StreamSource(new StringReader(JDK_DTD), systemID);
                }
                return null;
            }
        });

        XMLStreamReader xmlReader = factory.createXMLStreamReader(xmlSource);
        try {
            // move to the start tag
            while (!xmlReader.isStartElement()) {
                xmlReader.next();
            }
            xmlReader.require(XMLStreamConstants.START_ELEMENT, NS, "properties");

            block = parseBlock(xmlReader);

            xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, "properties");
        } finally {
            xmlReader.close();
        }

        return new PropertiesLoader(block);
    }

    private static ASTStatement parseBlock(XMLStreamReader xmlReader)
            throws XMLStreamException {
        ASTBlock block = new ASTBlock();

        // any <!ELEMENT xxx (entry|default|if|unless|choose|error)*> element
        String startTagName = xmlReader.getLocalName();
        while (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            String tagName = xmlReader.getLocalName();
            if ("entry".equals(tagName)) {
                block.add(parseEntry(xmlReader, false));

            } else if ("default".equals(tagName)) {
                block.add(parseEntry(xmlReader, true));

            } else if ("if".equals(tagName)) {
                block.add(parseIf(xmlReader, false, new ASTChoose()));

            } else if ("unless".equals(tagName)) {
                block.add(parseIf(xmlReader, true, new ASTChoose()));

            } else if ("choose".equals(tagName)) {
                block.add(parseChoose(xmlReader));

            } else if ("entryTable".equals(tagName)) {
                block.add(parseTable(xmlReader, false));

            } else if ("defaultTable".equals(tagName)) {
                block.add(parseTable(xmlReader, true));

            } else if ("matchPattern".equals(tagName)) {
                block.add(parsePattern(xmlReader));

            } else if ("error".equals(tagName)) {
                block.add(parseError(xmlReader));

            } else if ("comment".equals(tagName)) {
                // do nothing.  for jdk 1.5 properties XML format compatibility (which also ignores the comment)

            } else {
                throw new XMLStreamException("Unexpected tag: " + tagName, xmlReader.getLocation());
            }
        }
        xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, startTagName);

        return block;
    }

    private static ASTStatement parseEntry(XMLStreamReader xmlReader, boolean defaultOnly)
            throws XMLStreamException {
        // <!ELEMENT entry (#PCDATA)>
        // <!ATTLIST entry key CDATA #REQUIRED
        //                 value CDATA #IMPLIED
        //                 encrypted (true|false) #IMPLIED
        String keyTemplate = getRequiredAttribute(xmlReader, NS, "key");

        boolean encrypted = toBoolean(xmlReader.getAttributeValue(NS, "encrypted"));

        String valueTemplateAttr = xmlReader.getAttributeValue(NS, "value");
        String valueTemplateBody = xmlReader.getElementText().trim();
        if (valueTemplateAttr != null && valueTemplateBody.length() > 0) {
            throw new XMLStreamException("<" + xmlReader.getLocalName() + "> tag may not have both a value attribute and text in the body", xmlReader.getLocation());
        }
        String valueTemplate = (valueTemplateAttr != null) ? valueTemplateAttr : valueTemplateBody;

        ASTExpression keyExpression = parseTemplate(keyTemplate);
        ASTExpression valueExpression = parseTemplate(valueTemplate);
        return new ASTAssignment(keyExpression, valueExpression, defaultOnly, encrypted);
    }

    private static ASTStatement parseChoose(XMLStreamReader xmlReader)
            throws XMLStreamException {
        ASTChoose choose = new ASTChoose();

        // <!ELEMENT choose (when*,otherwise?)>
        String startTagName = xmlReader.getLocalName();
        while (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            String tagName = xmlReader.getLocalName();
            if ("when".equals(tagName)) {
                parseIf(xmlReader, false, choose);

            } else if ("unless".equals(tagName)) {
                parseIf(xmlReader, true, choose);

            } else if ("otherwise".equals(tagName)) {
                choose.setElse(parseBlock(xmlReader));

            } else {
                throw new XMLStreamException("Expected one of <when>, <otherwise>", xmlReader.getLocation());
            }
        }
        xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, startTagName);

        return choose;
    }

    private static ASTStatement parseIf(XMLStreamReader xmlReader, boolean invert, ASTChoose choose)
            throws XMLStreamException {
        //
        // parse the XML, either an "if" tag or a "when" tag
        //
        // <!ELEMENT if (%STMT;)>
        // <!ATTLIST if key CDATA #REQUIRED
        //             value CDATA #IMPLIED
        //             match CDATA #IMPLIED
        //             invert (true|false) #IMPLIED">
        String startTagName = xmlReader.getLocalName();

        String keyTemplate = getRequiredAttribute(xmlReader, NS, "key");

        String valueTemplate = xmlReader.getAttributeValue(NS, "value");
        String match = xmlReader.getAttributeValue(NS, "match");
        if (valueTemplate != null && match != null) {
            throw new XMLStreamException("<" + startTagName + "> tag may not have both value and match attributes", xmlReader.getLocation());
        }

        boolean caseSensitive = toBoolean(xmlReader.getAttributeValue(NS, "casesensitive"));

        invert ^= toBoolean(xmlReader.getAttributeValue(NS, "invert"));

        ASTStatement body = parseBlock(xmlReader);

        xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, startTagName);

        //
        // build the abstract syntax tree
        //
        ASTExpression keyExpression = parseTemplate(keyTemplate);

        ASTTest test;
        if (valueTemplate != null) {
            ASTExpression valueExpression = parseTemplate(valueTemplate);
            test = new ASTMapValueEquals(keyExpression, valueExpression, caseSensitive);
        } else if (match != null) {
            int regexFlags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            test = new ASTMapValueMatches(keyExpression, Pattern.compile(match, regexFlags));
        } else {
            test = new ASTMapValueTest(keyExpression);
        }

        if (invert) {
            test = new ASTNot(test);
        }

        choose.addWhen(test, body);

        return choose;
    }

    private static ASTStatement parseTable(XMLStreamReader xmlReader, boolean defaultOnly)
            throws XMLStreamException {

        // key is the name of the property being set
        String keyTemplate = getRequiredAttribute(xmlReader, NS, "key");
        ASTExpression keyExpression = parseTemplate(keyTemplate);

        // the separator delimits fields in the selector and <match> patterns
        String separator = xmlReader.getAttributeValue(NS, "separator");
        separator = (separator != null) ? Pattern.quote(separator) : "\0"; // \0 should never match anything
        Pattern separatorPattern = Pattern.compile(separator);

        // selector is the string that will be matched against each of the <match> patterns
        String selectorTemplate = getRequiredAttribute(xmlReader, NS, "selector");
        ASTExpression selectorExpression = parseTemplate(selectorTemplate);

        boolean caseSensitive = toBoolean(xmlReader.getAttributeValue(NS, "casesensitive"));

        ASTTable table = new ASTTable(selectorExpression, separatorPattern);

        String startTagName = xmlReader.getLocalName();
        while (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            String tagName = xmlReader.getLocalName();
            if (!"match".equals(tagName) && !"nomatch".equals(tagName)) {
                throw new XMLStreamException("Expected one of <when>, <otherwise>", xmlReader.getLocation());
            }

            String pattern = "match".equals(tagName) ? getRequiredAttribute(xmlReader, NS, "pattern") : null;

            boolean encrypted = toBoolean(xmlReader.getAttributeValue(NS, "encrypted"));

            String valueTemplateAttr = xmlReader.getAttributeValue(NS, "value");
            String valueTemplateBody = xmlReader.getElementText().trim();
            if (valueTemplateAttr != null && valueTemplateBody.length() > 0) {
                throw new XMLStreamException("<" + xmlReader.getLocalName() + "> tag may not have both a value attribute and text in the body", xmlReader.getLocation());
            }
            String valueTemplate = (valueTemplateAttr != null) ? valueTemplateAttr : valueTemplateBody;
            ASTExpression valueExpression = parseTemplate(valueTemplate);

            // when the entry matches, we do a regular assignment operation
            ASTAssignment stmt = new ASTAssignment(keyExpression, valueExpression, defaultOnly, encrypted);

            if (pattern != null) {
                // <match> tag with a regular expression pattern
                // wrap each field in parenthesis, replace separators with \n so .* won't cross separator boundaries
                pattern = "(" + separatorPattern.matcher(pattern).replaceAll(")\n(") + ")";
                // special case where a field with only '*' expands to '.*'
                pattern = pattern.replaceAll("(?m)^\\(\\*\\)$", "(.*)");
                int regexFlags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                table.addMatch(Pattern.compile(pattern, regexFlags), stmt);

            } else {
                // <nomatch> tag
                table.setNomatch(stmt);
            }
        }
        xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, startTagName);

        return table;
    }

    private static ASTStatement parsePattern(XMLStreamReader xmlReader)
            throws XMLStreamException {
        // <!ELEMENT matchPattern (%STMTS;)*>
        // <!ATTLIST matchPattern value CDATA #REQUIRED
        //                        match CDATA #REQUIRED
        //                        casesensitive (true|false) #IMPLIED>
        String startTagName = xmlReader.getLocalName();

        String valueTemplate = getRequiredAttribute(xmlReader, NS, "value");
        String match = getRequiredAttribute(xmlReader, NS, "match");
        boolean caseSensitive = toBoolean(xmlReader.getAttributeValue(NS, "casesensitive"));

        ASTStatement body = parseBlock(xmlReader);

        xmlReader.require(XMLStreamConstants.END_ELEMENT, NS, startTagName);

        //
        // build the abstract syntax tree
        //
        ASTExpression valueExpression = parseTemplate(valueTemplate);

        int regexFlags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(match, regexFlags);

        return new ASTMatchPattern(valueExpression, pattern, body);
    }

    private static ASTStatement parseError(XMLStreamReader xmlReader)
            throws XMLStreamException {
        String messageTemplateAttr = xmlReader.getAttributeValue(NS, "message");
        String messageTemplateBody = xmlReader.getElementText().trim();
        if (messageTemplateAttr != null && messageTemplateBody.length() > 0) {
            throw new XMLStreamException("<" + xmlReader.getLocalName() + "> tag may not have both a message attribute and text in the body", xmlReader.getLocation());
        }
        String messageTemplate = (messageTemplateAttr != null) ? messageTemplateAttr : messageTemplateBody;

        return new ASTError(parseTemplate(messageTemplate));
    }

    private static ASTExpression parseTemplate(String template) {
        return new ASTTemplate(new PropertiesPlaceholder(template));
    }

    private static String getRequiredAttribute(XMLStreamReader xmlReader, String namespace, String attrName)
            throws XMLStreamException {
        String attrValue = xmlReader.getAttributeValue(namespace, attrName);
        if (attrValue == null) {
            throw new XMLStreamException("<" + xmlReader.getLocalName() + "> tag without '" + attrName + "' attribute", xmlReader.getLocation());
        }
        return attrValue;
    }

    private static boolean toBoolean(String string) {
        return string != null && Boolean.parseBoolean(string);
    }

    private static String decrypt(String key, String value) {
        Assert.isNotNull(_decryptionAdapter, "Encountered encrypted value, but no decryptionAdapter was supplied.");
        try {
            return _decryptionAdapter.decryptString(key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //
    // Abstract syntax tree corresponding to the format of the configuration file.
    //

    private static interface ASTStatement {
        void execute(Properties map);
    }

    private static interface ASTExpression {
        String evaluate(Properties map);
    }

    private static interface ASTTest {
        boolean evaluate(Properties map);
    }

    /**
     * Assignment of "key=value" in the result map.
     */
    private static class ASTAssignment implements ASTStatement {
        private final ASTExpression _key;
        private final ASTExpression _value;
        private final boolean _default;
        private final boolean _encrypted;

        public ASTAssignment(ASTExpression key, ASTExpression value, boolean defaultOnly, boolean encrypted) {
            _key = key;
            _value = value;
            _default = defaultOnly;
            _encrypted = encrypted;
        }

        public void execute(Properties map) {
            String key = _key.evaluate(map);
            String value = map.getProperty(key, "");
            if (!_default || "".equals(value)) {
                value = _value.evaluate(map);
                if (_encrypted) {
                    value = decrypt(key, value);
                }
            }
            map.put(key, value);
        }
    }

    /**
     * Ordered sequence of ASTStatement objects.
     */
    private static class ASTBlock implements ASTStatement {
        private final List<ASTStatement> _stmts = new ArrayList<ASTStatement>();

        public void add(ASTStatement stmt) {
            _stmts.add(stmt);
        }

        public void execute(Properties map) {
            for (ASTStatement stmt : _stmts) {
                stmt.execute(map);
            }
        }
    }

    /**
     * Conditional block "if test1 then block1 else if test2 then block2 ... else blockN".
     */
    private static class ASTChoose implements ASTStatement {
        private final List<ASTTest> _whenTests = new ArrayList<ASTTest>();
        private final List<ASTStatement> _whenBlocks = new ArrayList<ASTStatement>();
        private ASTStatement _elseBlock;

        public void addWhen(ASTTest test, ASTStatement block) {
            _whenTests.add(test);
            _whenBlocks.add(block);
        }

        public void setElse(ASTStatement block) {
            _elseBlock = block;
        }

        public void execute(Properties map) {
            ASTStatement block = _elseBlock;
            for (int i = 0; i < _whenBlocks.size(); i++) {
                if (_whenTests.get(i).evaluate(map)) {
                    block = _whenBlocks.get(i);
                    break;
                }
            }
            if (block != null) {
                block.execute(map);
            }
        }
    }

    private static class ASTTable implements ASTStatement {
        private final ASTExpression _selector;
        private final Pattern _separator;
        private final List<Pattern> _patterns = new ArrayList<Pattern>();
        private final List<ASTStatement> _matches = new ArrayList<ASTStatement>();
        private ASTStatement _nomatch;

        public ASTTable(ASTExpression selector, Pattern separator) {
            _selector = selector;
            _separator = separator;
        }

        public void addMatch(Pattern pattern, ASTStatement match) {
            _patterns.add(pattern);
            _matches.add(match);
        }

        public void setNomatch(ASTStatement nomatch) {
            _nomatch = nomatch;
        }

        public void execute(Properties map) {
            String selector = _selector.evaluate(map);
            selector = _separator.matcher(selector).replaceAll("\n");
            ASTStatement block = _nomatch;
            for (int i = 0; i < _patterns.size(); i++) {
                if (_patterns.get(i).matcher(selector).matches()) {
                    block = _matches.get(i);
                    break;
                }
            }
            if (block != null) {
                block.execute(map);
            }
        }
    }

    private static class ASTMatchPattern implements ASTStatement {
        private final ASTExpression _value;
        private final Pattern _pattern;
        private final ASTStatement _body;

        public ASTMatchPattern(ASTExpression value, Pattern pattern, ASTStatement body) {
            _value = value;
            _pattern = pattern;
            _body = body;
        }

        public void execute(Properties map) {
            String value = _value.evaluate(map);
            Matcher matcher = _pattern.matcher(value);
            if (!matcher.matches()) {
                return;
            }
            
            // create temporary entries in the Properties named ${0}, ${1}, ${2} ... corresponding to the captured groups
            Map<String, Object> oldValues = new HashMap<String, Object>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                String key = Integer.toString(i);
                String group = matcher.group(i);
                Object old;
                if (group != null) {
                    old = map.put(key, group);
                } else {
                    old = map.remove(key);
                }
                oldValues.put(key, old);
            }

            _body.execute(map);

            // restore the temporary entries in the Properties named ${0}, ${1}, ${2} ...
            for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
                if (entry.getValue() != null) {
                    map.put(entry.getKey(), entry.getValue());
                } else {
                    map.remove(entry.getKey());
                }
            }
        }
    }

    /**
     * Throws an exception if this statement is ever evaluated.
     */
    private static class ASTError implements ASTStatement {
        private final ASTExpression _message;

        public ASTError(ASTExpression message) {
            _message = message;
        }

        public void execute(Properties map) {
            throw new PropertiesLoaderException(_message.evaluate(map));
        }
    }

    /**
     * Test if a value in the map has the value false (empty, "0" or "false").
     */
    private static class ASTMapValueTest implements ASTTest {
        private final ASTExpression _key;

        public ASTMapValueTest(ASTExpression key) {
            _key = key;
        }

        public boolean evaluate(Properties map) {
            return test(map.getProperty(_key.evaluate(map), ""));
        }

        private static boolean test(String string) {
            string = string.trim();
            return !string.equals("") && !string.equals("0") && !string.equalsIgnoreCase("false");
        }
    }

    /**
     * Test if a value in the map equals a particular string.
     */
    private static class ASTMapValueEquals implements ASTTest {
        private final ASTExpression _key;
        private final ASTExpression _value;
        private final boolean _caseSensitive;

        public ASTMapValueEquals(ASTExpression key, ASTExpression value, boolean caseSensitive) {
            _key = key;
            _value = value;
            _caseSensitive = caseSensitive;
        }

        public boolean evaluate(Properties map) {
            String actual = map.getProperty(_key.evaluate(map), "");
            if (_caseSensitive) {
                return actual.equals(_value.evaluate(map));
            } else {
                return actual.equalsIgnoreCase(_value.evaluate(map));
            }
        }
    }

    /**
     * Test if a value in the map matches a regular expression.
     */
    private static class ASTMapValueMatches implements ASTTest {
        private final ASTExpression _key;
        private final Pattern _pattern;

        public ASTMapValueMatches(ASTExpression key, Pattern pattern) {
            _key = key;
            _pattern = pattern;
        }

        public boolean evaluate(Properties map) {
            String actual = map.getProperty(_key.evaluate(map), "");
            return _pattern.matcher(actual).matches();
        }
    }

    private static class ASTNot implements ASTTest {
        private final ASTTest _test;

        public ASTNot(ASTTest test) {
            _test = test;
        }

        public boolean evaluate(Properties map) {
            return !_test.evaluate(map);
        }
    }

    /**
     * String template of interleaved string literals and variable expansions.
     * For example, the source strings of "abc${def}ghi${jkl}mno" and
     * "${abc}${def}" corresponds to the following:
     * <pre>
     *  new ASTTemplate({"abc", "ghi", "mno"}, {"def", "jkl"})
     *  new ASTTemplate({"", "", ""}, {"abc", "def"})
     * <pre>
     */
    private static class ASTTemplate implements ASTExpression {
        private final PropertiesPlaceholder _placeholder;

        public ASTTemplate(PropertiesPlaceholder placeholder) {
            _placeholder = placeholder;
        }

        public String evaluate(Properties map) {
            return _placeholder.expand(map);
        }
    }

    private static class ASTLiteral implements ASTExpression {
        private final String _string;

        public ASTLiteral(String string) {
            _string = string;
        }

        public String evaluate(Properties map) {
            return _string;
        }
    }
}
