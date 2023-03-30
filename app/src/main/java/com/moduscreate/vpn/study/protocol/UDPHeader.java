package com.moduscreate.vpn.study.protocol;

import androidx.annotation.NonNull;

import com.moduscreate.vpn.study.utils.BitUtils;

import java.nio.ByteBuffer;

public class UDPHeader {
    public int sourcePort;
    public int destinationPort;
    public int length;
    public int checksum;

    public UDPHeader() {}

    UDPHeader(ByteBuffer buffer) {
        this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
        this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

        this.length = BitUtils.getUnsignedShort(buffer.getShort());
        this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
    }

    void fillHeader(ByteBuffer buffer) {
        buffer.putShort((short) this.sourcePort);
        buffer.putShort((short) this.destinationPort);
        buffer.putShort((short) this.length);
        buffer.putShort((short) this.checksum);
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UDPHeader{");
        sb.append("sourcePort=").append(sourcePort);
        sb.append(", destinationPort=").append(destinationPort);
        sb.append(", length=").append(length);
        sb.append(", checksum=").append(checksum);
        sb.append('}');
        return sb.toString();
    }
}