package com.bazaarvoice.prr.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Simple writer collapses adjacent whitespace characters (space, tab, newline, carriage return)
 * into a single whitespace character.  There is no special logic to handle <pre>, <script>, or
 * Javascript strings specially, so don't use this on a page where that's likely to cause
 * formatting problems such as on an error page).
 */
public class CollapseWhitespaceWriter extends FilterWriter {

    /** True if the last char was whitespace, so we shouldn't write out the next char if it's also whitespace. */
    private boolean _ignoreWhitespace;

    public CollapseWhitespaceWriter(Writer writer) {
        super(writer);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        // this is performance sensitive so we go through a lot of work to avoid calling super.write() more times than necessary
        boolean ignoreWhitespace = _ignoreWhitespace;
        boolean prevSpaceOrNewline = false;  // irrelevant (not kept up-to-date) when ignoreWhitespace is true
        int start = 0;                       // irrelevant (not kept up-to-date) when ignoreWhitespace is true
        for (int i = 0; i < len; i++) {
            char c = cbuf[off + i];
            if (ignoreWhitespace) {
                // ignoring whitespace.  check if we should stop
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    ignoreWhitespace = false;
                    start = i;
                    prevSpaceOrNewline = false; // c is not whitespace
                }
            } else if (prevSpaceOrNewline && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
                // write the chars up to this point including the previous space or
                // newline but not the current whitespace character
                if (i > start) {
                    super.write(cbuf, off + start, i - start); // includes prev space or newline, but not c
                }
                ignoreWhitespace = true;
            } else if (c == '\t') {
                // write the string up to this point but not including the tab
                if (i > start) {
                    super.write(cbuf, off + start, i - start);
                }
                super.write(' '); // writing space converts tab to space
                ignoreWhitespace = true;
            } else if (c == '\r') {
                // write the string up to this point but not including the carriage return
                if (i > start) {
                    super.write(cbuf, off + start, i - start);
                }
                super.write('\n'); // writing newline converts carriage return to newline
                ignoreWhitespace = true;
            } else {
                // postpone writing c until we encounter whitespace or the end of the array.
                // if this is a space or a newline, we'll write it out like any other character,
                // but next time through the loop if there's another space or newline we'll ignore it.
                prevSpaceOrNewline = (c == ' ' || c == '\n');
            }
        }
        if (!ignoreWhitespace && len > start) {
            super.write(cbuf, off + start, len - start);
        }
        _ignoreWhitespace = ignoreWhitespace || prevSpaceOrNewline;
    }

    public void write(String string, int off, int len) throws IOException {
        // this is performance sensitive so we go through a lot of work to avoid calling super.write() more times than necessary
        boolean ignoreWhitespace = _ignoreWhitespace;
        boolean prevSpaceOrNewline = false;  // irrelevant (not kept up-to-date) when ignoreWhitespace is true
        int start = 0;                       // irrelevant (not kept up-to-date) when ignoreWhitespace is true
        for (int i = 0; i < len; i++) {
            char c = string.charAt(off + i);
            if (ignoreWhitespace) {
                // ignoring whitespace.  check if we should stop
                if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                    ignoreWhitespace = false;
                    start = i;
                    prevSpaceOrNewline = false; // c is not whitespace
                }
            } else if (prevSpaceOrNewline && (c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
                // write the chars up to this point including the previous space or
                // newline but not the current whitespace character
                if (i > start) {
                    super.write(string, off + start, i - start); // includes prev space or newline, but not c
                }
                ignoreWhitespace = true;
            } else if (c == '\t') {
                // write the string up to this point but not including the tab
                if (i > start) {
                    super.write(string, off + start, i - start);
                }
                super.write(' '); // writing space converts tab to space
                ignoreWhitespace = true;
            } else if (c == '\r') {
                // write the string up to this point but not including the carriage return
                if (i > start) {
                    super.write(string, off + start, i - start);
                }
                super.write('\n'); // writing newline converts carriage return to newline
                ignoreWhitespace = true;
            } else {
                // postpone writing c until we encounter whitespace or the end of the array.
                // if this is a space or a newline, we'll write it out like any other character,
                // but next time through the loop if there's another space or newline we'll ignore it.
                prevSpaceOrNewline = (c == ' ' || c == '\n');
            }
        }
        if (!ignoreWhitespace && len > start) {
            super.write(string, off + start, len - start);
        }
        _ignoreWhitespace = ignoreWhitespace || prevSpaceOrNewline;
    }

    public void write(int c) throws IOException {
        if (c == ' ' || c == '\t') {
            // tab and space collapse to space, suppress subsequent whitespace
            if (!_ignoreWhitespace) {
                _ignoreWhitespace = true;
                super.write(' ');
            }
        } else if (c == '\r' || c == '\n') {
            // newline and carriage return collapse to a newline, suppress subsequent whitespace
            if (!_ignoreWhitespace) {
                _ignoreWhitespace = true;
                super.write('\n');
            }
        } else {
            _ignoreWhitespace = false;
            super.write(c);
        }
    }
}
