package com.moduscreate.vpn.study.utils;

import com.moduscreate.vpn.study.protocol.IP4Header;
import com.moduscreate.vpn.study.protocol.Packet;
import com.moduscreate.vpn.study.protocol.TCPHeader;
import com.moduscreate.vpn.study.protocol.TransportProtocol;
import com.moduscreate.vpn.study.protocol.UDPHeader;

import java.net.InetSocketAddress;

public class IpUtil {
    /**
     * Build UDP packet, sent to device.
     */
    public static Packet buildUdpPacket(InetSocketAddress source, InetSocketAddress dest, int ipId) {
        Packet packet = new Packet();
        packet.isTCP = false;
        packet.isUDP = true;

        IP4Header ip4Header = getIPv4Header(source, dest, ipId, TransportProtocol.UDP);

        UDPHeader udpHeader = new UDPHeader();
        udpHeader.sourcePort = source.getPort();
        udpHeader.destinationPort = dest.getPort();
        udpHeader.length = 0; // this will be updated in fun sendUdpPacket

        packet.ip4Header = ip4Header;
        packet.udpHeader = udpHeader;

        return packet;
    }

    private static IP4Header getIPv4Header(InetSocketAddress source, InetSocketAddress dest, int ipId, TransportProtocol protocol) {
        IP4Header ip4Header = new IP4Header();
        ip4Header.version = 4; // IPv4 header
        ip4Header.IHL = 5; // (4 bits) IP header length, min value if 5 and max value is 15. So 5x4 = 20, 15x4 = 60 as header length.
        ip4Header.destinationAddress = dest.getAddress();
        ip4Header.headerChecksum = 0;
        ip4Header.headerLength = 20; // Header length: IHL * 4 (bits, size of IHL) = 20

        int ipFlag = 0x40;
        int ipOff = 0;

        ip4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;
        ip4Header.optionsAndPadding = 0;
        ip4Header.protocol = protocol;
        ip4Header.protocolNum = (short) protocol.getNumber();
        ip4Header.sourceAddress = source.getAddress();
        ip4Header.totalLength = 0;
        ip4Header.typeOfService = 0;
        ip4Header.TTL = 64;

        return ip4Header;
    }

    /**
     * Build TCP packet, sent to device.
     */
    public static Packet buildTcpPacket(InetSocketAddress source, InetSocketAddress dest, byte flag, long ack, long seq, int ipId) {
        Packet packet = new Packet();
        packet.isTCP = true;
        packet.isUDP = false;

        IP4Header ip4Header = getIPv4Header(source, dest, ipId, TransportProtocol.TCP);

        TCPHeader tcpHeader = new TCPHeader();
        tcpHeader.acknowledgementNumber = ack;
        tcpHeader.checksum = 0;
        tcpHeader.dataOffsetAndReserved = 0;
        tcpHeader.destinationPort = dest.getPort();
        tcpHeader.flags = flag;
        tcpHeader.headerLength = 0;
        tcpHeader.optionsAndPadding = null;
        tcpHeader.sequenceNumber = seq;
        tcpHeader.sourcePort = source.getPort();
        tcpHeader.urgentPointer = 0;
        tcpHeader.window = 65535;

        packet.ip4Header = ip4Header;
        packet.tcpHeader = tcpHeader;

        return packet;
    }
}