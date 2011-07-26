package com.bazaarvoice.prr.util;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * A very simple Excel CSV writer.
 *
 * @author Andy Maag
 *
 */
public class ExcelCSVWriter {

    private PrintWriter pw;
    private char separator;
    private char quotechar;

    /** The character used for escaping quotes. */
    public static final char ESCAPE_CHARACTER = '\\';

    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';

    /** The default quote character to use if none is supplied to the constructor. */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     */
    public ExcelCSVWriter(Writer writer) {
        this(writer, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    public ExcelCSVWriter(Writer writer, char separator) {
        this(writer, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public ExcelCSVWriter(Writer writer, char separator, char quotechar) {
        this.pw = new PrintWriter(writer);
        this.separator = separator;
        this.quotechar = quotechar;
    }

    /**
     * Writes the entire list to a CSV file. The list is assumed to be
     * a String[]
     *
     * @param allLines a List of String[], with each String[] representing a line of the
     *         file.
     *
     * @throws IOException
     *             if bad things happen during the write
     */
    public void writeAll(List allLines) throws IOException {

        for (Iterator iter = allLines.iterator(); iter.hasNext(); ) {
            String[] nextLine = (String[]) iter.next();
            writeNext(nextLine);
        }

    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a separate
     *         entry.
     *
     * @throws IOException
     *             if bad things happen during the write
     */
    public void writeNext(String[] nextLine) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nextLine.length; i++) {
            String nextElement = nextLine[i];

            // \r characters are problematic to Excel.  First try to convert \r\n to \n
            nextElement = StringUtils.replace(nextElement, "\r\n", "\n");
            // Next, just convert remaining \r to \n
            nextElement = StringUtils.replaceChars(nextElement, '\r', '\n');
            // Excel has a limit of 32K chars for a text field, but also seemed to have difficulty on shorter fields (~31K)
            nextElement = StringUtils.substring(nextElement, 0, 30000);

            sb.append(quotechar);
            for (int j = 0; j < nextElement.length(); j++) {
                char nextChar = nextElement.charAt(j);
                if (nextChar == quotechar) {
                    sb.append(quotechar).append(nextChar);
                } else {
                    sb.append(nextChar);
                }
            }
            sb.append(quotechar);
            if (i != nextLine.length - 1) {
                sb.append(separator);
            }
        }
        sb.append('\n');
        pw.write(sb.toString());
    }
}
