package com.bazaarvoice.prr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple Excel CSV reader.
 *
 * @author Andy Maag
 */
public class ExcelCSVReader {

    private BufferedReader br;
    private boolean hasNext = true;
    private char separator;
    private char quotechar;
    private int lineNumber;

    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';

    /** The default quote character to use if none is supplied to the constructor. */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * Constructs CSVReader using a comma for the separator.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     */
    public ExcelCSVReader(Reader reader) {
        this(reader, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVReader with supplied separator.
     *
     * @param reader the reader to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    public ExcelCSVReader(Reader reader, char separator) {
        this(reader, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader the reader to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public ExcelCSVReader(Reader reader, char separator, char quotechar) {
        this.br = new BufferedReader(reader);
        this.separator = separator;
        this.quotechar = quotechar;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     *         file.
     *
     * @throws java.io.IOException
     *             if bad things happen during the read
     */
    public List<String[]> readAll() throws IOException {

        List<String[]> allElements = new ArrayList<String[]>();
        while (hasNext) {
            String[] nextLineAsTokens = readNext();
            if (nextLineAsTokens != null)
                allElements.add(nextLineAsTokens);
        }
        return allElements;

    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     *         entry.
     *
     * @throws IOException
     *             if bad things happen during the read
     */
    public String[] readNext() throws IOException {

        String nextLine = getNextLine();
        return hasNext ? parseLine(nextLine) : null;
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException if bad things happen during the read
     */
    private String getNextLine() throws IOException {
        lineNumber++;
        String nextLine = br.readLine();
        if (nextLine == null) {
            hasNext = false;
        }
        return hasNext ? nextLine : null;
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine
     *            the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException
     */
    private String[] parseLine(String nextLine) throws IOException {

        if (nextLine == null) {
            return null;
        }

        int i = 0;
        try {
            List<String> tokensOnThisLine = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            boolean inQuotes = false;
            do {
                if (inQuotes) {
                    // continuing a quoted section, reappend newline
                    sb.append("\n");
                    nextLine = getNextLine();
                    if (nextLine == null)
                        break;
                }

                for (i = 0; i < nextLine.length(); i++) {

                    char c = nextLine.charAt(i);
                    if (c == quotechar) {
                        // If we're in quotes, peek ahead one character to see if it is a double quote.
                        if (inQuotes && i < nextLine.length() - 1 && nextLine.charAt(i + 1) == quotechar) {
                            // A double quote within quotes is read as a single quote.  Add the quote to the input
                            // stream and skip the 2nd quote character.
                            sb.append(quotechar);
                            i++;
                        } else {
                            inQuotes = !inQuotes;
                        }
                    } else if (c == separator && !inQuotes) {
                        tokensOnThisLine.add(sb.toString());
                        sb = new StringBuilder(); // start work on next token
                    } else {
                        sb.append(c);
                    }
                }
            } while (inQuotes);
            tokensOnThisLine.add(sb.toString());
            return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);
        } catch(Exception e) {
            throw new IOException("Parse error on line " + lineNumber + " on column " + i + ": " + e.getMessage());
        }
    }

}
