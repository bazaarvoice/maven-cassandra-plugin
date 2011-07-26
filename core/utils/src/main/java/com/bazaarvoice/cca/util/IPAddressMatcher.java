package com.bazaarvoice.cca.util;

import org.springframework.beans.factory.annotation.Required;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to check matching one string against patterns from this
 * class
 *
 * @author ilyasch
 *
 */
public class IPAddressMatcher {

    private boolean _includeLoopback;
    private boolean _includeSiteLocal;
    private List<IPAddressRange> _addressRanges = new ArrayList<IPAddressRange>();
    private List<IPAddressRange> _proxyAddressRanges = new ArrayList<IPAddressRange>();

    public IPAddressMatcher() {
        // do nothing.  The patterns should be initialized via the setter
    }

    @Required
    public void setIncludeLoopback(boolean includeLoopback) {
        _includeLoopback = includeLoopback;
    }

    @Required
    public void setIncludeSiteLocal(boolean includeSiteLocal) {
        _includeSiteLocal = includeSiteLocal;
    }

    @Required
    public void setAddresses(List<String> ipAddressRanges) {
        _addressRanges = new ArrayList<IPAddressRange>(ipAddressRanges.size());
        for (String ipAddressRange : ipAddressRanges) {
            _addressRanges.add(IPAddressRange.parseCidrAddress(ipAddressRange));
        }
    }

    @Required
    public void setProxyAddresses(List<String> ipAddressRanges) {
        _proxyAddressRanges = new ArrayList<IPAddressRange>(ipAddressRanges.size());
        for (String ipAddressRange : ipAddressRanges) {
            _proxyAddressRanges.add(IPAddressRange.parseCidrAddress(ipAddressRange));
        }
    }
    /**
     * Checks the request remote address against the allowed list of addresses.  If
     * the address matches at least one, returns <code>true</code> otherwise returns
     * <code>false</code>
     */
    public boolean matches(ServletRequest request) {
        // the ServletRequest.getRemoteAddress() is always an IP address, not a DNS name
        String remoteAddressString = request.getRemoteAddr();

        // parse the IP address into an InetAddress object.
        InetAddress remoteAddress;
        try {
            remoteAddress = InetAddress.getByName(remoteAddressString);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unable to parse IP address: " + remoteAddressString);
        }

        if (matchesProxy(remoteAddress)) {
            InetAddress originalRemoteAddress = originalAddress(request);
            if (originalRemoteAddress != null) {
                remoteAddress = originalRemoteAddress;
            }
        }
        
        return matches(remoteAddress);
    }

    public boolean matches(InetAddress address) {
        // since this function uses InetAddress it works with IPv4 and IPv6 addresses.  IPv6 
        // addresses can be encountered in local testing especially w/the loopback address.
        if (_includeLoopback && address.isLoopbackAddress()) {
            return true; // 127.x.x.x, ::1
        }

        if (_includeSiteLocal && address.isSiteLocalAddress()) {
            return true; // 10.x.x.x, 192.168.x.x, 172.16.x.x/12, FEC0::/10
        }

        for (IPAddressRange addressRange : _addressRanges) {
            if (addressRange.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the address is one of the approved proxy addresses
     */
    public boolean matchesProxy(InetAddress address) {
        for (IPAddressRange addressRange : _proxyAddressRanges) {
            if (addressRange.contains(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the IP address of the original request. This is useful information when the request
     * has been proxied to a staging environment, such as AWS-ALTSTG
     */
    public InetAddress originalAddress(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            String forwardedAddressString = ((HttpServletRequest) request).getHeader("X-Forwarded-For");
            if (forwardedAddressString == null) {
                return null;
            }

            // Intentionally excluding multiple forwarding hops for now
            String[] forwardedAddresses = forwardedAddressString.split(",\\s*");
            forwardedAddressString = forwardedAddresses[forwardedAddresses.length - 1];
           
            // parse the IP address into an InetAddress object.
            try {
                return InetAddress.getByName(forwardedAddressString);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unable to parse IP address: " + forwardedAddressString);
            }
        }

        return null;
    }
}
