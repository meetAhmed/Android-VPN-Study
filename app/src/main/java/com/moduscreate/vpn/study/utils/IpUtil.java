package com.moduscreate.vpn.study.utils;

import com.moduscreate.vpn.study.protocol.IP4Header;
import com.moduscreate.vpn.study.protocol.Packet;
import com.moduscreate.vpn.study.protocol.TransportProtocol;
import com.moduscreate.vpn.study.protocol.UDPHeader;

import java.net.InetSocketAddress;

public class IpUtil {
    public static Packet buildUdpPacket(InetSocketAddress source, InetSocketAddress dest, int ipId) {
        Packet packet = new Packet();
        packet.isTCP = false;
        packet.isUDP = true;

        IP4Header ip4Header = new IP4Header();
        ip4Header.version = 4;
        ip4Header.IHL = 5;
        ip4Header.destinationAddress = dest.getAddress();
        ip4Header.headerChecksum = 0;
        ip4Header.headerLength = 20;

        //int ipId=0;
        int ipFlag = 0x40;
        int ipOff = 0;

        ip4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;
        ip4Header.optionsAndPadding = 0;
        ip4Header.protocol = TransportProtocol.UDP;
        ip4Header.protocolNum = 17;
        ip4Header.sourceAddress = source.getAddress();
        ip4Header.totalLength = 60;
        ip4Header.typeOfService = 0;
        ip4Header.TTL = 64;

        UDPHeader udpHeader = new UDPHeader();
        udpHeader.sourcePort = source.getPort();
        udpHeader.destinationPort = dest.getPort();
        udpHeader.length = 0;

        packet.ip4Header = ip4Header;
        packet.udpHeader = udpHeader;

        return packet;
    }
}
