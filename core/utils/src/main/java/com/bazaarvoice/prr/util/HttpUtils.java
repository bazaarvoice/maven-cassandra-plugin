package com.bazaarvoice.prr.util;

import com.bazaarvoice.cca.util.IOUtils;
import com.bazaarvoice.cca.util.NoneAvailableInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HttpUtils {
    private static final Log _sLog = LogFactory.getLog(HttpUtils.class);

    // Connection parameters for opening a URL to a server that may be down temporarily
    private static final long URL_OPEN_INITIAL_WAIT_TIME = 5 * DateUtils.MILLIS_PER_SECOND;
    private static final int URL_OPEN_BACKOFF_FACTOR = 2;

    private static final Pattern CHARSET = Pattern.compile("\\bcharset=([-a-zA-Z0-9]+)");

    /**
     * Adds headers to the response which set the CDN time-to-live (TTL) to 15 hours.
     * @param response the response
     */
    public static void addCdn15HourTimeToLive(HttpServletResponse response) {
        // The cache-maxage value supports several suffixes for the time period:
        // Edge-control: cache-maxage=90m   - defines 90 minute TTL
        // Edge-control: cache-maxage=1h    - defines 1 hour TTL
        // Edge-control: cache-maxage=5d    - defines 5 days TTL
        response.setHeader("Edge-Control", "cache-maxage=15h");
    }

    public static void addCdnTimeToLiveInSeconds(HttpServletResponse response, int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("CDN TTL must be positive, not " + seconds);
        }
        response.setHeader("Edge-Control", "cache-maxage=" + seconds + "s");
    }

    /**
     * Adds headers to the response which will keep the CDN from caching the response
     * This means that all requests for this URL will be forwarded to the origin
     * @param response the response
     */
    public static void addCdnNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Edge-Control", "no-store");
    }

    /**
     * Adds headers to the response which will keep the end-user's browser from caching the response
     * This means that all requests for this URL will be forwarded to the CDN
     * @param response the response
     * @see org.springframework.web.servlet.mvc.AbstractCommandController#setCacheSeconds(int)
     */
    public static void addClientNoCacheHeaders(HttpServletResponse response) {
        // see http://www.mnot.net/cache_docs/ for an explanation of all the cache control headers

        // HTTP 1.0 header
        response.setDateHeader("Expires", 1L);

        // HTTP 1.1 headers: "no-cache" is the standard value,
        // "no-store" is necessary to prevent caching on FireFox.
        response.setHeader("Cache-Control", "no-cache, no-store");
    }

    /**
     * Gets the IP Address of the end-user that initiated the request
     * @param request the request
     * @return the IP Address of the end-user
     */
    public static String getRemoteIP(HttpServletRequest request) {

        // (See Ticket #10434 for details)
        // We previously looked for the end-user IP in one of two places.
        // First we look for a special "True-Client-IP" header that Akamai adds to requests.
        // Second, if that header is not found, we then fall back to the regular HTTP request remove IP address.
        //
        // The problem with this is that some of our customers send all of the end-user traffic
        // through a proxy server that the client hosts.
        // In these cases, the "True-Client-IP" that Akamai adds is actually the address of the proxy server,
        // rather than the address of the end-user.
        //
        // To support this case, we add a third option to our IP lookup.
        // We should first look for a new header called "BV-True-Client-IP".
        // This way, clients who implement a proxy server can put the end-user IP into this header
        // and Akamai will pass it on to us without overwriting it with their own header.
        // The priority of lookup should be:
        //    1. "BV-True-Client-IP"
        //    2. "True-Client-IP"
        //    3. HttpRequest.getRemoteAddr()

        String ipAddress = request.getHeader("BV-True-Client-IP");

        if (StringUtils.isEmpty(ipAddress)) {
            ipAddress = request.getHeader("True-Client-IP");
        }

        // If we didn't find the special headers, just use the RemoteAddress of the request
        if (StringUtils.isEmpty(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        return ipAddress;
    }

    /**
     * Determines whether the browser type is Safari.
     *
     * @param request the request
     * @return true if user agent is Safari
     */
    public static boolean isSafari(HttpServletRequest request) { // Used via OGNL.
        return StringUtils.containsIgnoreCase(getUserAgent(request), "safari");
    }

    /**
     * Determines whether the browser type is Internet Explorer.
     *
     * @param request the request
     * @return true if user agent is Internet Explorer
     */
    public static boolean isExplorer(HttpServletRequest request) { // Used via OGNL.
        return StringUtils.containsIgnoreCase(getUserAgent(request), "msie");
    }

    /**
     * Gets the user agent of the requester
     *
     * @param request the request
     *
     * @return The user agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public static boolean isTextContentType(String contentType) {
        return contentType.startsWith("text/") ||
               "application/x-javascript".equals(contentType) ||
               "application/x-www-form-urlencoded".equals(contentType);
    }

    /**
     * Returns an open HttpURLConnection associated with the URL, waiting a while for the server
     * to come online if it appears to be down at the moment.  This is designed for
     * hitting our own Bazaarvoice servers.
     */
    public static HttpURLConnection connectReliably(URL url, String requestMethod, long maxWaitTimeMillis) throws IOException {
        return connectReliably(url, requestMethod, null, maxWaitTimeMillis);
    }

    public static HttpURLConnection connectReliably(URL url, String requestMethod, Map<String, String> requestHeaders, long maxWaitTimeMillis) throws IOException {
        long start = System.currentTimeMillis();
        long waitTime = URL_OPEN_INITIAL_WAIT_TIME;
        for (;;) {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (requestMethod != null) {
                urlConnection.setRequestMethod(requestMethod);
            }
            if (requestHeaders != null) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            Exception exception;
            try {

                try {
                    urlConnection.getInputStream(); // make the connection and throw an exception if it fails
                } finally {
                    closeErrorStreamQuietly(urlConnection);  // we never read error pages--throw them away
                }
                return urlConnection;

            } catch (ConnectException ce) {
                // socket couldn't make a connection.  fall through to the retry logic
                exception = ce;

            } catch (IOException ioe) {
                if (urlConnection.getResponseCode() != HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
                    throw ioe;
                }
                // 503 response means the server might be starting or stopping or temporarily overloaded.  fall through to the retry logic
                exception = ioe;
            }

            // couldn't open the connection to the server
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > maxWaitTimeMillis) {
                // we've waited long enough.  fail.
                throw new IOException("Unable to access '" + url + "': " + exception, exception);
            }

            // maybe the server is being restarted.  wait a while and try again
            _sLog.warn("Unable to access url, waiting " + Math.round(waitTime / 1000.0) + " seconds for server to come online: '" + url + "'; " + exception);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            // wait longer next time (but don't wait too long)
            waitTime *= URL_OPEN_BACKOFF_FACTOR;
            elapsed = System.currentTimeMillis() - start;
            if (waitTime > maxWaitTimeMillis - elapsed) {
                waitTime = Math.max(maxWaitTimeMillis - elapsed, 0) + 1;
            }
        }
    }

    public static InputStream openReliably(URL url, long maxWaitTimeMillis) throws IOException {
        return getInputStream(connectReliably(url, "GET", maxWaitTimeMillis));
    }

    public static InputStream getInputStream(HttpURLConnection urlConnection) throws IOException {
        // work around performance bugs in the JDK's ChunkedInputStream using NoneAvailableInputStream
        // in case the input stream gets wrapped by a BufferedInputStream by the caller
        return new NoneAvailableInputStream(urlConnection.getInputStream());
    }

    public static String readUrlResponseString(HttpURLConnection cxn) throws IOException {
        try {
            // try to extract the character set from the Content-Type header
            String characterEncoding = null;
            String contentType = cxn.getContentType();
            if (contentType != null) {
                Matcher matcher = CHARSET.matcher(contentType);
                if (matcher.find()) {
                    characterEncoding = StringUtils.trimToNull(matcher.group(1));
                }
            }
            if (characterEncoding == null) {
                characterEncoding = "UTF-8";
            }

            InputStream in = cxn.getInputStream();
            try {
                return IOUtils.toString(in, characterEncoding);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            InputStream err = cxn.getErrorStream();
            if (err != null) {
                throw new IOException(IOUtils.toString(err), e);
            } else {
                throw e;
            }
        } finally {
            closeErrorStreamQuietly(cxn);
        }
    }

    public static void closeErrorStreamQuietly(HttpURLConnection urlConnection) {
        IOUtils.closeQuietly(urlConnection.getErrorStream());
    }
}
