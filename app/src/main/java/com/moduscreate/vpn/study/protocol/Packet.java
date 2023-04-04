package com.moduscreate.vpn.study.protocol;

import androidx.annotation.NonNull;

import com.moduscreate.vpn.study.utils.BitUtils;
import com.moduscreate.vpn.study.utils.SimpleLogger;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class Packet {
    public static final int IP4_HEADER_SIZE = 20;
    public static final int TCP_HEADER_SIZE = 20;
    public static final int UDP_HEADER_SIZE = 8;

    public static final AtomicLong globalPackId = new AtomicLong();
    public IP4Header ip4Header;
    public TCPHeader tcpHeader;
    public UDPHeader udpHeader;
    public ByteBuffer backingBuffer;

    public boolean isTCP;
    public boolean isUDP;

    public Packet() {
        globalPackId.addAndGet(1);
    }

    public Packet(ByteBuffer buffer) throws UnknownHostException {
        this();
        this.ip4Header = new IP4Header(buffer);
        if (this.ip4Header.protocol == TransportProtocol.TCP) {
            this.tcpHeader = new TCPHeader(buffer);
            this.isTCP = true;
        } else if (ip4Header.protocol == TransportProtocol.UDP) {
            this.udpHeader = new UDPHeader(buffer);
            this.isUDP = true;
        }
        this.backingBuffer = buffer;
    }

    public void release() {
        ip4Header = null;
        tcpHeader = null;
        udpHeader = null;
        backingBuffer = null;
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Packet{");
        sb.append("ip4Header=").append(ip4Header);
        if (isTCP) sb.append(", tcpHeader=").append(tcpHeader);
        else if (isUDP) sb.append(", udpHeader=").append(udpHeader);
        sb.append(", payloadSize=").append(backingBuffer.limit() - backingBuffer.position());
        sb.append('}');
        return sb.toString();
    }

    public ByteBuffer updateTCPBuffer(byte flags, long sequenceNum, long ackNum, byte[] payload) {
        // Creating byte buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(IP4_HEADER_SIZE + TCP_HEADER_SIZE + (payload == null ? 0 : payload.length));

        if (payload != null) {
            byteBuffer.position(IP4_HEADER_SIZE + TCP_HEADER_SIZE);
            byteBuffer.put(payload);
        }

        // move buffer to position 0
        byteBuffer.position(0);

        // insert tcp header
        fillHeader(byteBuffer);

        tcpHeader.flags = flags;

        byteBuffer.put(IP4_HEADER_SIZE + 13, flags);

        tcpHeader.sequenceNumber = sequenceNum;

        byteBuffer.putInt(IP4_HEADER_SIZE + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNumber = ackNum;

        byteBuffer.putInt(IP4_HEADER_SIZE + 8, (int) ackNum);

        // Reset header size, since we don't need options
        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.dataOffsetAndReserved = dataOffset;
        byteBuffer.put(IP4_HEADER_SIZE + 12, dataOffset);

        updateTCPChecksum(payload == null ? 0 : payload.length, byteBuffer);

        int ip4TotalLength = IP4_HEADER_SIZE + TCP_HEADER_SIZE + (payload == null ? 0 : payload.length);
        byteBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum(byteBuffer);

        byteBuffer.position(ip4TotalLength);

        return byteBuffer;
    }

    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
        // move buffer to position 0
        buffer.position(0);

        // insert udp header
        fillHeader(buffer);

        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP4_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;

        // Disable UDP checksum validation
        backingBuffer.putShort(IP4_HEADER_SIZE + 6, (short) 0);
        udpHeader.checksum = 0;

        // Update total length of IPv4
        int ip4TotalLength = IP4_HEADER_SIZE + udpTotalLength;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum(backingBuffer);
    }

    private void updateIP4Checksum(ByteBuffer byteBuffer) {
        ByteBuffer buffer = byteBuffer.duplicate();
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = ip4Header.headerLength;
        int sum = 0;
        while (ipLength > 0) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        ip4Header.headerChecksum = sum;
        byteBuffer.putShort(10, (short) sum);
    }

    private void updateTCPChecksum(int payloadSize, ByteBuffer byteBuffer) {
        int sum = 0;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        // Calculate pseudo-header checksum
        ByteBuffer buffer = ByteBuffer.wrap(ip4Header.sourceAddress.getAddress());
        sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        buffer = ByteBuffer.wrap(ip4Header.destinationAddress.getAddress());
        sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        sum += TransportProtocol.TCP.getNumber() + tcpLength;

        buffer = byteBuffer.duplicate();
        // Clear previous checksum
        buffer.putShort(IP4_HEADER_SIZE + 16, (short) 0);

        // Calculate TCP segment checksum
        buffer.position(IP4_HEADER_SIZE);
        while (tcpLength > 1) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            tcpLength -= 2;
        }
        if (tcpLength > 0)
            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        tcpHeader.checksum = sum;
        byteBuffer.putShort(IP4_HEADER_SIZE + 16, (short) sum);
    }

    private void fillHeader(ByteBuffer buffer) {
        ip4Header.fillHeader(buffer);
        if (isUDP)
            udpHeader.fillHeader(buffer);
        else if (isTCP)
            tcpHeader.fillHeader(buffer);
    }
}
