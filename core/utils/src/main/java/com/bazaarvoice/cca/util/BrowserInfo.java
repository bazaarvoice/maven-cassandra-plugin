package com.bazaarvoice.cca.util;

/**
 * Contains the browser type and version
 */
public class BrowserInfo {
    public enum BrowserType { IE, CHROME, SAFARI, WEBKIT, FIREFOX, MOZILLA, OPERA, UNKNOWN }

    public static final String UNKNOWN_VERSION = "0";
    public static final BrowserInfo UNKNOWN_BROWSER = new BrowserInfo(BrowserType.UNKNOWN, UNKNOWN_VERSION, "");

    private BrowserType _type;
    private String _version;
    private String _userAgent;

    public BrowserInfo(BrowserType type, String version, String userAgent) {
        _type = type;
        _version = version;
        _userAgent = userAgent;
    }

    public BrowserType getType() {
        return _type;
    }

    public String getVersion() {
        return _version;
    }

    public String getUserAgent() {
        return _userAgent;
    }
}
