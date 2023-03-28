package com.moduscreate.vpn.study.protocol;

import com.moduscreate.vpn.study.utils.BitUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * B = Bits
 * IHL = IP header length
 * TOS = Type of service
 * TTL = Time to live
 * Protocol = Name of the protocol to which the data is to be passed (8 bits)
 *
 * +------------------------------------------------------------+
 * |      4B       |     4B    |     8B    |        16B         |
 * |    Version    |    IHL    |    TOS    |    Total Length    |
 * +------------------------------------------------------------+
 * |             16B               |      3B     |     13B      |
 * |        Identification         |    Flags    |    Offset    |
 * +------------------------------------------------------------+
 * |     8B    |       8B       |              16B              |
 * |    TTL    |    Protocol    |           Checksum            |
 * +------------------------------------------------------------+
 * |                             32B                            |
 * |                       Source Address                       |
 * +------------------------------------------------------------+
 * |                             32B                            |
 * |                    Destination Address                     |
 * +------------------------------------------------------------+
 * |                             32B                            |
 * |                          Options                           |
 * +------------------------------------------------------------+
 * |                                                            |
 * |                            DATA                            |
 * |                                                            |
 * +------------------------------------------------------------+
 */
public class IP4Header {
    public byte version;
    public byte IHL;
    public int headerLength;
    public short typeOfService;
    public int totalLength;

    public int identificationAndFlagsAndFragmentOffset;

    public short TTL;
    public short protocolNum;
    public TransportProtocol protocol;
    public int headerChecksum;

    public InetAddress sourceAddress;
    public InetAddress destinationAddress;

    public int optionsAndPadding;

    public enum TransportProtocol {
        TCP(6),
        UDP(17),
        Other(0xFF);

        private final int protocolNumber;

        TransportProtocol(int protocolNumber) {
            this.protocolNumber = protocolNumber;
        }

        private static TransportProtocol numberToEnum(int protocolNumber) {
            if (protocolNumber == 6)
                return TCP;
            else if (protocolNumber == 17)
                return UDP;
            else
                return Other;
        }

        public int getNumber() {
            return this.protocolNumber;
        }
    }

    public IP4Header() {}

    IP4Header(ByteBuffer buffer) throws UnknownHostException {
        byte versionAndIHL = buffer.get();
        this.version = (byte) (versionAndIHL >> 4);
        this.IHL = (byte) (versionAndIHL & 0x0F);
        this.headerLength = this.IHL << 2;

        this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
        this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

        this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

        this.TTL = BitUtils.getUnsignedByte(buffer.get());
        this.protocolNum = BitUtils.getUnsignedByte(buffer.get());
        this.protocol = TransportProtocol.numberToEnum(protocolNum);
        this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());

        byte[] addressBytes = new byte[4];
        buffer.get(addressBytes, 0, 4);
        this.sourceAddress = InetAddress.getByAddress(addressBytes);

        buffer.get(addressBytes, 0, 4);
        this.destinationAddress = InetAddress.getByAddress(addressBytes);

        //this.optionsAndPadding = buffer.getInt();
    }

    public void fillHeader(ByteBuffer buffer) {
        buffer.put((byte) (this.version << 4 | this.IHL));
        buffer.put((byte) this.typeOfService);
        buffer.putShort((short) this.totalLength);

        buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

        buffer.put((byte) this.TTL);
        buffer.put((byte) this.protocol.getNumber());
        buffer.putShort((short) this.headerChecksum);

        buffer.put(this.sourceAddress.getAddress());
        buffer.put(this.destinationAddress.getAddress());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IP4Header{");
        sb.append("version=").append(version);
        sb.append(", IHL=").append(IHL);
        sb.append(", typeOfService=").append(typeOfService);
        sb.append(", totalLength=").append(totalLength);
        sb.append(", identificationAndFlagsAndFragmentOffset=").append(identificationAndFlagsAndFragmentOffset);
        sb.append(", TTL=").append(TTL);
        sb.append(", protocol=").append(protocolNum).append(":").append(protocol);
        sb.append(", headerChecksum=").append(headerChecksum);
        sb.append(", sourceAddress=").append(sourceAddress.getHostAddress());
        sb.append(", destinationAddress=").append(destinationAddress.getHostAddress());
        sb.append('}');
        return sb.toString();
    }
}
