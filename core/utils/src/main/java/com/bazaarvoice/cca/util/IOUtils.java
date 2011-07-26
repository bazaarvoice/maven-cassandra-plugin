package com.bazaarvoice.cca.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

public abstract class IOUtils extends org.apache.commons.io.IOUtils {
    private static final Log _sLog = LogFactory.getLog(IOUtils.class);

    public static final String GZIP_SUFFIX = ".gz";

    public static BufferedReader openUTF8InputFile(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    }

    public static LineWriter openUTF8OutputFile(File file) throws IOException {
        return openUTF8OutputFile(file, false);
    }

    public static LineWriter openUTF8OutputFile(File file, boolean append) throws IOException {
        return new LineWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8")));
    }

    public static File newTempFile(File tempDirectoryPath) throws IOException {
        return newTempFile("bvapp", ".txt", tempDirectoryPath);
    }

    public static File newTempFile(String prefix, String suffix, File tempDirectoryPath) throws IOException {
        return File.createTempFile(prefix, suffix, tempDirectoryPath);
    }

    public static File newTempFile(Collection<File> allTempFiles, File tempDirectoryPath) throws IOException {
        File tempFile = newTempFile(tempDirectoryPath);
        allTempFiles.add(tempFile);
        return tempFile;
    }

    public static void deleteTempFiles(Collection<File> filesToDelete) {
        for (File file : filesToDelete) {
            if (!file.delete()) {
                _sLog.warn("Unable to delete temporary file: " + file);
            }
        }
    }

    /**
     * Creates the parent directories for the given File, and returns the given File.
     *
     * @param file The file to create directories for
     * @throws IOException if the parent directory doesn't exist and cannot be created.
     */
    public static void createDirectoriesForFile(File file)
            throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
            if (!parentDir.exists()) {
                throw new IOException("Could not create directory '" + parentDir + "' for file '" + file + "'.");
            }
        }
        if (!parentDir.isDirectory()) {
            throw new IOException("Path '" + parentDir + "' is not a directory.");
        }
    }

    public static synchronized void renameTo(File intermediateFile, File outputFile) throws IOException {
        // synchronized to eliminate all race conditions between different threads of this same Java VM
        if (outputFile.exists() && !outputFile.delete() && outputFile.exists()) {
            throw new IOException("Unable to remove pre-existing output file: " + outputFile);
        }
        if (!intermediateFile.renameTo(outputFile)) {
            throw new IOException("Unable to rename temporary file to destination output file: " + intermediateFile + " -> " + outputFile);
        }
    }

    public static BufferedInputStream gunzipIfNecessary(BufferedInputStream in) throws IOException {
        if (isGZipped(in)) {
            return new BufferedInputStream(new GZIPInputStream(in));
        }
        return in;
    }

    /**
     * Returns true if the specified input stream is compressed using the gzip compression algorithm.
     * @param in an InputStream that supports mark() and reset() such as BufferedInputStream.
     * @return true if the specified input stream is compressed using the gzip compression algorithm. 
     */
    public static boolean isGZipped(InputStream in) throws IOException {
        // according to http://www.gzip.org/zlib/rfc-gzip.html all gzip streams start with 0x1f 0x8b
        return hasHeader(in, new byte[] {(byte) 0x1f, (byte) 0x8b});
    }

    /**
     * Returns true if the specified input stream is a zip archive.  Note this format includes jar
     * files and other zip-compressed documents such as some Office 2007 documents.
     * @param in an InputStream that supports mark() and reset() such as BufferedInputStream.
     * @return true if the specified input stream is compressed using the gzip compression algorithm.
     */
    public static boolean isZipped(InputStream in) throws IOException {
        // according to http://www.garykessler.net/library/file_sigs.html all zip files start with 50 4B 03 04
        return hasHeader(in, new byte[] {(byte)0x50, (byte)0x4b, (byte)0x03, (byte)0x04});
    }

    private static boolean hasHeader(InputStream in, byte[] header) throws IOException {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("InputStream must support the mark() method: " + in.getClass().getName());
        }
        in.mark(header.length);  // remember where we are so we can read ahead, then rewind
        try {
            for (byte headerByte : header) {
                int b = in.read();
                if (b == -1 || (byte) b != headerByte) {
                    return false;
                }
            }
            return true;
        } finally {
            in.reset(); // move the stream pointer back to where it was when we started
        }
    }

    /**
     * Reads up to <tt>len</tt> bytes from the specified input stream into the specified buffer.
     * This method will always read <tt>len</tt> bytes unless it encounters the end of the file.
     * This is contrast to the regular InputStream.read method which may read fewer than <tt>len</tt>
     * bytes for a number of different reasons.  For example, it may read only the bytes that are
     * immediately available.
     * <p>
     * This method is similar to DataInputStream.readFully() except that it returns -1 on EOF
     * instead of throwing EOFException. 
     */
    public static int readFully(InputStream in, byte[] b) throws IOException {
        return readFully(in, b, 0, b.length);
    }

    /**
     * Reads up to <tt>len</tt> bytes from the specified input stream into the specified buffer.
     * This method will always read <tt>len</tt> bytes unless it encounters the end of the file.
     * This is contrast to the regular InputStream.read method which may read fewer than <tt>len</tt>
     * bytes for a number of different reasons.  For example, it may read only the bytes that are
     * immediately available. 
     */
    public static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count == -1) {
                return (n > 0) ? n : -1;
            }
            n += count;
        }
        return n;
    }

    /**
     * Copy the contents of an input stream to an output stream, ignoring IOExceptions from the
     * output stream such as HTTP servlet client disconnects.
     */
    public static void copyIgnoreOutputIOExceptions(InputStream in, OutputStream out) throws IOException {
        copyIgnoreOutputIOExceptions(in, out, Long.MAX_VALUE);
    }

    /**
     * Copy  up to <tt>max</tt> bytes from an input stream to an output stream, ignoring IOExceptions
     * from the output stream such as HTTP servlet client disconnects.
     * @return the number of bytes that were read, or -1 if an exception was encountered on the output stream
     */
    public static long copyIgnoreOutputIOExceptions(InputStream in, OutputStream out, long maxRead) throws IOException {
        byte[] buf = new byte[(int) Math.min(4096, maxRead)];
        long numRead = 0;
        while (numRead < maxRead) {
            int count = in.read(buf, 0, (int) Math.min(buf.length, maxRead - numRead));
            if (count == -1) {
                break; // eof on input
            }
            numRead += count;
            try {
                out.write(buf, 0, count);
            } catch (IOException e) {
                // sliently ignore I/O exceptions on output (e.g. client disconnects)
                return -1;
            }
        }
        return numRead;
    }

    /**
     * Similar to FileUtils.copyInputStreamToFile() except it closes the output stream without suppressing IOExceptions.
     * Note: this closes the input stream!
     */
    public static void copyInputStreamToFile(InputStream in, File file) throws IOException {
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                copyLarge(in, out);
            } finally {
                out.close(); // don't closeQuietly--propagate exceptions while closing the output file
            }
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * Opens a file that may or may not end with .gz.
     *
     * @param directory the location where the file should be
     * @param fileName  the filename of the file in @directory
     * @return null a File object, or null if file is not found
     */
    public static File getMaybeGzippedFile(File directory, String fileName) {
        boolean hasGzSuffix = fileName.endsWith(GZIP_SUFFIX);

        // locate the file on disk.  first look with .gz extension (usual location) then w/o .gz extension (alternate location)
        if (!hasGzSuffix) {
            fileName += GZIP_SUFFIX;
        }
        File file = new File(directory, fileName);
        if (!file.isFile()) {
            // didn't find it with a .gz extension, so try without a .gz extension
            file = new File(StringUtils.removeEnd(file.getPath(), GZIP_SUFFIX));
            if (!file.isFile()) {
                return null;
            }
        }

        return file;
    }

    public static byte[] md5(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            MessageDigest md = DigestUtils.newMD5Digest();
            byte[] buf = new byte[16384];
            int len;
            while ((len = in.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            return md.digest();
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * URL constructor that throws a RuntimeException instead of a checked MalformedURLException.
     */
    public static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(url, e);
        }
    }
}
