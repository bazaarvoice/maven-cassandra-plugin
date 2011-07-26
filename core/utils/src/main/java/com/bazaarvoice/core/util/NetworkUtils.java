package com.bazaarvoice.core.util;

import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Utility functions for network activity.
 *
 * @author chris
 * @author Alex Araujo
 */
public class NetworkUtils {

    private static final String IP_ADDRESS_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(IP_ADDRESS_REGEX);

    /**
     * Returns whether or not the string is in the format of an IP address.
     *
     * @param value The string to be checked.
     * @return whether or not the string is formatted properly for an IP address.
     */
    public static boolean isIPAddress(String value) {
        return IP_ADDRESS_PATTERN.matcher(value).matches();
    }

    /**
     * Normalize a host:port combination by stripping off default port numbers etc.
     */
    public static String getNormalizedHostAndPort(String scheme, String hostAndPort) {
        if ("http".equals(scheme)) {
            hostAndPort = StringUtils.chomp(hostAndPort, ":80");
        } else if ("https".equals(scheme)) {
            hostAndPort = StringUtils.chomp(hostAndPort, ":443");
        }
        return StringUtils.lowerCase(hostAndPort);
    }

    /**
     * Given a string like "localhost:8081", returns a string like "127.0.0.1:8081".
     */
    public static String getHostIPAddressAndPort(String hostNameAndPort) throws UnknownHostException {
        int portSuffixPosition = hostNameAndPort.indexOf(':');
        if (portSuffixPosition == -1) {
            portSuffixPosition = hostNameAndPort.length();
        }
        String hostName = hostNameAndPort.substring(0, portSuffixPosition);
        String portSuffix = hostNameAndPort.substring(portSuffixPosition);
        String ipAddress = InetAddress.getByName(hostName).getHostAddress();
        return ipAddress + portSuffix;
    }
    
    /**
     * Return canonical name of the host or it IP, when name cannot be determined, as string.
     */
    public static String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch(Throwable e){
            try {
                return java.net.InetAddress.getLocalHost().getHostAddress();
            } catch(Throwable ex){
                return "UNKNOWN";
            }
        }
    }
}
