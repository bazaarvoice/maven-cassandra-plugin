package com.bazaarvoice.cca.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Wraps a Writer with an API similar to PrintWriter.println() except that
 * it always uses '\n' as the line terminator to be more compatible with
 * MySQL LOAD DATA INFILE on Windows.  Also doesn't hide exceptions
 * the way PrintWriter does.
 */
public class LineWriter implements Closeable {
    private final Writer _out;

    public LineWriter(Writer out) {
        _out = out;
    }

    public void println(String line) throws IOException {
        print(line);
        println();
    }

    public void print(String s) throws IOException {
        _out.write(s);
    }

    public void print(char ch) throws IOException {
        _out.write(ch);
    }

    public void println() throws IOException {
        _out.write('\n');  // always use \n, never \r\n, for MySQL LOAD DATA compatibility
    }

    public void close() throws IOException {
        _out.close();
    }
}
