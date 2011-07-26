package com.bazaarvoice.core.web.util;

import com.bazaarvoice.core.util.NetworkUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Common functions used when interacting with the Servlet API.
 */
public abstract class ServletApiUtils {
    /**
     * Returns a request path that is relative to the context of the servlet.
     * This path will contain any servlet mapping path plus the path of the request inside the servlet
     *
     * @param request the request
     * @return a request path that is relative to the context of the servlet.
     */
    public static String getRequestPath(HttpServletRequest request) {
        return request.getServletPath() + StringUtils.defaultString(request.getPathInfo());
    }

    /**
     * Gets an attribute out of an HttpSession, if one exists.
     */
    public static Object getSessionAttribute(HttpServletRequest request, String attributeName) {
        HttpSession session = request.getSession(false);
        return (session != null) ? session.getAttribute(attributeName) : null;
    }

    /**
     * Sets an attribute in an HttpSession, creating a session for the user if necessary.
     */
    public static void setSessionAttribute(HttpServletRequest request, String attributeName, Object attributeValue) {
        request.getSession(true).setAttribute(attributeName, attributeValue);
    }

    /**
     * Returns the normalized hostname and port combination used by the browser to
     * make an HTTP request, if available.
     *
     * @param request HTTP servlet request object.
     * @return The normalized hostname and port combination used by the browser to make an HTTP request, if available.
     */
    public static String getRequestHostAndPort(HttpServletRequest request) {
        String hostAndPort = request.getHeader("Host");
        if (hostAndPort == null) {
            hostAndPort = request.getLocalAddr() + ":" + request.getServerPort();
        }
        return NetworkUtils.getNormalizedHostAndPort(request.getScheme(), hostAndPort);
    }

    /**
     * Returns the normalized local IP address and port combination, which may be
     *  used to make loopback requests
     */
    public static String getLocalAddressAndPort(HttpServletRequest request) {
        String localAddress = request.getLocalAddr() != null ? request.getLocalAddr() : "127.0.0.1";
        return NetworkUtils.getNormalizedHostAndPort(request.getScheme(), localAddress + ":" + request.getServerPort());
    }

    /**
     * Appends parameter to the existing url
     *
     * @param url        The URL to be added with parameter.
     * @param paramName  HTTP parameter name.
     * @param paramValue HTTP parameter value.
     * @return The url with appended parameter.
     */
    public static String appendParameter(String url, String paramName, String paramValue) {
        if (!StringUtils.isEmpty(paramValue)) {
            return url + ((url.indexOf('?') >= 0) ? "&" : "?") + paramName + "=" + urlEncode(paramValue);
        } else {
            return url;
        }
    }

    /**
     * Appends parameter to the existing url
     *
     * @param url        The URL to be added with parameter.
     * @param paramName  HTTP parameter name.
     * @param paramValue HTTP parameter value.
     * @return The url with appernded parameter.
     */
    public static StringBuilder appendParameter(StringBuilder url, String paramName, String paramValue) {
        if (!StringUtils.isEmpty(paramValue)) {
            url.append((url.indexOf("?") >= 0) ? "&" : "?");
            url.append(paramName);
            url.append("=");
            url.append(urlEncode(paramValue));
        }
        return url;
    }

    /**
     * URL encodes the specified string using the UTF-8 encoding.
     *
     * @param url The URL to be encoded.
     * @return The encoded URL.
     */
    public static String urlEncode(String url) {
        try {
            if(url != null) {
                //According to the spec (http://www.ietf.org/rfc/rfc2396.txt), the plus '+' character is valid for the path portion of a URL,
                //but it represents an encoded space ' ' character in the query string portion of a URL.
                //However, java.net.URLEncoder always encodes space characters as pluses.
                //Since we don't know which portion of the URL we are encoding for, we should be overly aggressive and always encode incoming space characters as '%20'
                url = URLEncoder.encode(url, "UTF-8");
                //After calling URLEncoder.encode, all true '+' characters will already be encoded as '%2B' and any remaining '+' characters will represent encoded spaces
                url = StringUtils.replace(url, "+", "%20");
            }

            return url;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // UTF-8 should always be supported
        }
    }

    /**
     * URL decodes the specified string using the UTF-8 encoding.
     *
     * @param url The URL to be decoded.
     * @return The decoded URL.
     */
    public static String urlDecode(String url, boolean decodeForPath) {
        try {
            if(url != null) {
                //According to the spec (http://www.ietf.org/rfc/rfc2396.txt), the plus '+' character is valid for the path portion of a URL,
                //but it represents an encoded space ' ' character in the query string portion of a URL.
                //However, java.net.URLDecoder always decodes '+' characters as spaces
                //We need to use different behavior depending on whether the URL is in the query string or not
                if(decodeForPath) {
                    //Since URLDecoder.decode() will always change '+' characters to spaces, we should change '+' characters to '%2B'
                    url = StringUtils.replace(url, "+", "%2B");
                }

                url = URLDecoder.decode(url, "UTF-8");
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // UTF-8 should always be supported
        }
    }

    /**
     * Convert a String which may not conform to the URL and URI specs to a conformant URI.
     *
     * Each section of a URL mandates a slightly different set of characters that must be encoded.  To achieve the correct encoding, the String is
     * first parsed as a URL.  The components are run through URLDecoder (as appropriate) to remove one layer of encoding.  The URI class is then passed
     * each parsed component of the URL, which it encodes according to the allowable character set for each component.  This avoids the over-encoding
     * that occurs if the URLEncoder class were to be used.
     */
    public static URI toConformantURI(String url) {
        try {
            URL parsedUrl = new URL(url);
            // Must use the multi-argument constructor, since the single-argument one does not encode.
            return new URI(parsedUrl.getProtocol(),
                    urlDecode(parsedUrl.getUserInfo(), false),
                    urlDecode(parsedUrl.getHost(), false),
                    parsedUrl.getPort(),
                    urlDecode(parsedUrl.getPath(), true),
                    urlDecode(parsedUrl.getQuery(), false),
                    urlDecode(parsedUrl.getRef(), false));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse and encode " + url + " as a valid URL.", e);
        }
    }

    /**
     * Parses a string of URL parameters such as the string returned by URL.getQuery().
     *
     * @param parameters A string that represents HTTP parameters.
     * @return Parsed HTTP parameter values keyed by parameter names.
     */
    public static Map<String, String[]> parseURLParameters(String parameters) {
        Map<String, String[]> map = new LinkedHashMap<String, String[]>();
        if (parameters != null) {
            Map<String, List<String>> multiMap = new LinkedHashMap<String, List<String>>();
            for (String paramNameValue : StringUtils.split(parameters, '&')) {
                String name = urlDecode(StringUtils.substringBefore(paramNameValue, "="), false);
                String value = urlDecode(StringUtils.substringAfter(paramNameValue, "="), false);
                List<String> values = multiMap.get(name);
                if (values == null) {
                    multiMap.put(name, values = new ArrayList<String>());
                }
                values.add(value);
            }
            for (Map.Entry<String, List<String>> entry : multiMap.entrySet()) {
                String name = entry.getKey();
                List<String> values = entry.getValue();
                map.put(name, values.toArray(new String[values.size()]));
            }
        }
        return map;
    }

    public static void redirectPermanently(String targetUrl, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", response.encodeRedirectURL(targetUrl));
    }

    public static void redirectSeeOther(String targetUrl, HttpServletRequest request, HttpServletResponse response) {
        String protocol = request.getProtocol();
        if (protocol == null || protocol.endsWith("/0.9") || protocol.endsWith("/1.0")) {
            // old clients may not know how to deal with 303, so use 302 redirect code.
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        } else {
            // new HTTP clients since HTTP/1.1 interpret 303 correctly
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        }
        response.setHeader("Location", response.encodeRedirectURL(targetUrl));
    }
}
