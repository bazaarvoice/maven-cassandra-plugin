package com.bazaarvoice.cca.util;

import org.apache.commons.lang.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * A range of IPv4 addresses consisting of a base IP address and a subnet mask, as
 * described in http://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing.
 * <p>
 * For example: the IP range 192.168.0.0/16 includes 192.168.0.0 through 192.168.255.255.
 */
public class IPAddressRange {

    private final int _ipAddress;  // unsigned int
    private final int _subnetMask; // unsigned int

    public IPAddressRange(int ipAddress) {
        this(ipAddress, 32);
    }

    public IPAddressRange(int ipAddress, int subnetMaskLength) {
        _subnetMask = subnetMaskLengthToMask(subnetMaskLength);
        _ipAddress = ipAddress & _subnetMask;  // zero out insignificant bits
    }

    /**
     * Parses a CIDR address range and returns the IPRange values.  If the mask length is left off then 32 is defaulted.
     *
     * @param cidrAddress CIDR address of format 192.168.0.0/16
     */
    public static IPAddressRange parseCidrAddress(String cidrAddress)
            throws NumberFormatException {

        String[] parts = StringUtils.split(cidrAddress, "/");

        if (parts.length != 1 && parts.length != 2) {
            throw new NumberFormatException("address must be of the form 'a.b.c.d/x': " + cidrAddress);
        }

        // parse the IP portion of the address range
        int ipValue = ipToUnsignedInt(parts[0]);

        if (parts.length == 1) {
            // eg. 192.168.1.1
            // range containing a single IP address
            return new IPAddressRange(ipValue);

        } else {
            // eg. 192.168.0.0/16
            // parse the subnet mask length portion of the address range
            int subnetMaskLength = Integer.parseInt(parts[1]);
            if (subnetMaskLength < 0 || subnetMaskLength > 32) {
                throw new NumberFormatException("address mask must be 0-32: " + subnetMaskLength);
            }
            return new IPAddressRange(ipValue, subnetMaskLength);
        }
    }

    /**
     * Returns the 32-bit value of this ip address.  The input should be of format a.b.c.d
     * If this can't be parsed, a NumberFormatException is thrown.
     */
    public static int ipToUnsignedInt(String ipAddress)
            throws NumberFormatException {
        String[] ipParts = StringUtils.split(ipAddress, ".");
        if (ipParts == null || ipParts.length != 4) {
            throw new NumberFormatException("address must contain 4 numerical parts: " + ipAddress);
        }
        int[] intParts = new int[4];
        for (int i = 0; i < 4; i++) {
            intParts[i] = Integer.parseInt(ipParts[i]);
            if (intParts[i] < 0 || intParts[i] > 255) {
                throw new NumberFormatException("address parts must be 0-255: " + intParts[i]);
            }
        }

        return octetsToUnsignedInt(intParts[0], intParts[1], intParts[2], intParts[3]);

    }

    public long getNumAddresses() {
        // invert the mask to get all 'insignificant' bits, add one to include the first value in the range
        // examples:
        //  mask == 0 means 2^32 addresses (note: this must be stored in a long)
        //  mask == 128.0.0.0 means 2^31 addresses (note: this must be stored in a long)
        //  mask == 255.255.192.0 means 2^14 addresses
        //  mask == 255.255.255.255 means 1 address
        return unsignedIntToLong(~_subnetMask) + 1L;
    }

    public long getLow() {
        return unsignedIntToLong(_ipAddress);
    }

    public long getHigh() {
        return unsignedIntToLong(_ipAddress | ~_subnetMask);
    }

    public boolean contains(String ipAddress)
            throws NumberFormatException {
        return contains(ipToUnsignedInt(ipAddress));
    }

    public boolean contains(InetAddress address) {
        // only ipv4 ranges are supported.  ipv6 ranges return false.
        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int value = octetsToUnsignedInt(bytes[0], bytes[1], bytes[2], bytes[3]);
            return contains(value);
        }
        return false;
    }

    private boolean contains(int address) {
        return _ipAddress == (address & _subnetMask);
    }

    @Override
    public boolean equals(Object obj) {
        // this is used for test case mostly
        if (!(obj instanceof IPAddressRange)) {
            return false;
        }
        IPAddressRange range = (IPAddressRange) obj;
        return _ipAddress == range._ipAddress && _subnetMask == range._subnetMask;
    }

    @Override
    public String toString() {
        // if the subnet mask is created correctly, the mask length is the number of bits in the mask
        int subnetMaskLength = Integer.bitCount(_subnetMask);

        return ((_ipAddress >>> 24) & 0xFF) + "." +
               ((_ipAddress >>> 16) & 0xFF) + "." +
               ((_ipAddress >>> 8) & 0xFF) + "." +
               (_ipAddress & 0xFF) + "/" +
               subnetMaskLength;
    }

    private static int subnetMaskLengthToMask(int subnetMaskLength) {
        if (subnetMaskLength < 0 || subnetMaskLength > 32) {
            throw new IllegalArgumentException("subnet mask length must be 0-32: " + subnetMaskLength);
        }
        // examples:
        //  maskLen == 0 means anything goes, mask = 0.0.0.0
        //  maskLen == 32 means all bits must match, mask = 255.255.255.255
        //  maskLen == 18 means first 18 bits must match, mask = 255.255.192.0
        if (subnetMaskLength == 0) {
            return 0;
        }
        int numInsignificantBits = 32 - subnetMaskLength;
        return 0xFFFFFFFF << numInsignificantBits;
    }

    private static int octetsToUnsignedInt(int a, int b, int c, int d) {
        return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((c & 0xFF) << 8) | (d & 0xFF);
    }

    private static long unsignedIntToLong(int unsignedInt) {
        return ((long) unsignedInt) & 0xFFFFFFFFL;
    }
}
