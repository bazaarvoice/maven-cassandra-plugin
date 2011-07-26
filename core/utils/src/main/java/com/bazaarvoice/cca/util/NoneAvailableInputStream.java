package com.bazaarvoice.cca.util;

import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream proxy that works around a bug in the JDK's ChunkedInputStream when it is wrapped
 * with a BufferedInputStream.  ChunkedInputStream is used by URLConnection.openStream() against
 * an HTTP server that uses the chunked encoding (ie. when the Content-Length header isn't set).
 * <p>
 * The bug occurs because BufferedInputStream calls ChunkedInputStream.available() frequently,
 * and each call to ChunkedInputStream.available() internally calls readAhead() which reads and
 * buffers all the available data on the underlying HttpInputStream.  If the remote HTTP server
 * is at all fast, this quickly leads to two problems:
 * <ol>
 * <li> the ChunkedInputStream reads and buffers way ahead of the thread that's consuming the
 *   BufferedInputStream, leading ChunkedInputStream to buffer almost the entire result in
 *   memory.  This can blow out memory usage for a large feed file (100s of MB).
 * <li> the algorithm the ChunkedInputStream uses to resize its internal 'chunkData' buffer
 *   is O(n^2) once it starts to buffer large amounts of data, leading to *really* slow
 *   performance.
 * </ol>
 * This class works around the problem by always never calling ChunkedInputStream.available()
 * and always returning zero to BufferedInputStream.  The performance impact to client code is
 * negligible, and it avoids all the bad behavior of ChunkedInputStream.available().
 */
public class NoneAvailableInputStream extends ProxyInputStream {

    public NoneAvailableInputStream(InputStream proxy) {
        super(proxy);
    }

    @Override
    public int available() throws IOException {
        return 0;
    }
}
