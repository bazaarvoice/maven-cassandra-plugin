package com.bazaarvoice.cca.util;

import com.bazaarvoice.core.util.BVStringUtils;
import com.bazaarvoice.prr.util.DateUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProcessUtils {
    private static final Log _sLog = LogFactory.getLog(ProcessUtils.class);

    private static final String LINUX_PROC_UPTIME = "/proc/uptime";

    private static final String CMD_EC2_METADATA = "/usr/local/bin/ec2-metadata";

    /**
     * If we can't get OS uptime, estimate it by starting from JVM start time and working back 10 minutes.
     * (OS uptime is only implemented for Linux so this will be used on Windows and OS/X.)
     */
    private static final long ESTIMATED_MACHINE_TO_JVM_START_MILLIS = 10 * DateUtils.MILLIS_PER_MINUTE;

    /**
     * Run the specified command and throw an exception if it returns a non-zero exit code.
     */
    public static void execProcess(String... cmdline) throws IOException, InterruptedException {
        // start the process
        Process process = new ProcessBuilder(cmdline).start();

        // close stdin and stdout--don't provide any input and throw away the output.
        process.getOutputStream().close();
        process.getInputStream().close();

        // capture stderr in case the process fails.  assume platform default char encoding.
        InputStream stderr = process.getErrorStream();
        StringWriter errBuf = new StringWriter();
        IOUtils.copy(stderr, errBuf);
        stderr.close();

        // wait for the process to complete
        int exitCode = process.waitFor();

        // assume a non-zero exit code means the process failed
        if (exitCode != 0) {
            throw new IOException("Failure code " + exitCode + " executing command '" + StringUtils.join(cmdline, ' ') + "': " + errBuf.toString());
        }
    }

    /**
     * Run the specified command, throw an exception if it returns a non-zero exit code, and return
     * the process output.
     */
    public static String execProcessCaptureOutput(String... cmdline) throws IOException, InterruptedException {
        return execProcessCaptureOutput(null, cmdline);
    }

    /**
     * Run the specified command, throw an exception if it returns a non-zero exit code, and return
     * the process output.
     *
     * @param workingDirectory if null, will use the current working directory.  See also {@link ProcessBuilder#directory(java.io.File)}.
     */
    public static String execProcessCaptureOutput(File workingDirectory, String... cmdline) throws IOException, InterruptedException {
        // start the process
        Process process = new ProcessBuilder(cmdline).directory(workingDirectory).start();

        // close stdin--don't provide any input
        process.getOutputStream().close();

        // capture stdout and stderr.  assume platform default char encoding.
        CaptureOutput stdout = new CaptureOutput(process.getInputStream());
        CaptureOutput stderr = new CaptureOutput(process.getErrorStream());

        // wait for the process to complete
        int exitCode = process.waitFor();

        // assume a non-zero exit code means the process failed
        if (exitCode != 0) {
            throw new IOException("Failure code " + exitCode + " executing command '" + StringUtils.join(cmdline, ' ') + "': " + stderr.getContent());
        }

        return stdout.getContent();
    }

    /**
     * Return the timestamp when the machine booted, equivalent to the Linux 'uptime' command.  For operating
     * systems other than Linux, this returns an approximation based on how long the JVM has been alive.
     */
    public static long getMachineUptimeMillis(long jvmStartMilliseconds) {
        File uptimeFile = new File(LINUX_PROC_UPTIME);
        if (uptimeFile.exists() || "Linux".equals(System.getProperty("os.name"))) {  // check os.name so we warn if /proc/uptime doesn't work on Linux
            try {
                // the 'uptime' command is hard to parse.  easier to read second counts from /proc/uptime
                String uptimeLine = FileUtils.readFileToString(uptimeFile).trim();

                // the uptimeLine looks like "350735.47 234388.90" where the first number is the total
                // number of seconds the system has been up and the second number is the total number
                // of seconds the system as been idle.  we only care about the first number.
                String uptimeString = StringUtils.substringBefore(uptimeLine, " ");
                return Math.round(Double.parseDouble(uptimeString) * DateUtils.MILLIS_PER_SECOND);

            } catch (Exception e) {
                _sLog.warn("Unable to check processor uptime by reading file: " + uptimeFile, e);
            }
        }

        // Not Linux, can't measure uptime using "/proc/uptime", so approximate uptime using time
        // since the JVM started + some padding.  Assume the JVM started 10 minutes after the OS did.
        // This might be high, but it's designed for EC2 where "billing rounds up to the nearest hour"
        // means it's better to overestimate uptime and shutdown a machine a little early than vice versa.
        return System.currentTimeMillis() - jvmStartMilliseconds + ESTIMATED_MACHINE_TO_JVM_START_MILLIS;
    }

    /**
     * Returns a Map with the output of the "ec2-metadata" program.
     */
    public static Map<String, String> getAmazonEC2Metadata() {
        File ec2MetadataCmd = new File(CMD_EC2_METADATA);
        if (!ec2MetadataCmd.exists()) {
            return Collections.emptyMap(); // not EC2
        }

        Map<String, String> metadata = new HashMap<String, String>();
        try {
            String output = execProcessCaptureOutput(CMD_EC2_METADATA);
            for (String line : BVStringUtils.EOLN.split(output)) {
                if (!line.startsWith("\t")) {
                    String key = StringUtils.trimToNull(StringUtils.substringBefore(line, ": "));
                    String value = StringUtils.trimToNull(StringUtils.substringAfter(line, ": "));
                    if (key != null && value != null) {
                        metadata.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            _sLog.warn("Unable to get EC2 metadata by executing command: " + ec2MetadataCmd, e);
        }
        return metadata;
    }

    private static class CaptureOutput implements Runnable {
        private final Reader _in;
        private final Thread _thread;
        private final StringWriter _buf = new StringWriter();

        private CaptureOutput(InputStream in) throws UnsupportedEncodingException {
            _in = new InputStreamReader(in);
            _thread = new Thread(this);
            _thread.start();
        }

        public void run() {
            try {
                IOUtils.copy(_in, _buf);
            } catch (IOException e) {
                _sLog.error(e);
            } finally {
                IOUtils.closeQuietly(_in);
            }
        }

        public String getContent() throws InterruptedException {
            _thread.join(); // wait until the run() method has completed and returned
            return _buf.toString();
        }
    }
}
