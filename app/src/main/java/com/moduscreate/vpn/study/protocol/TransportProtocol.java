package com.moduscreate.vpn.study.protocol;

public enum TransportProtocol {
    TCP(6),
    UDP(17),
    Other(0xFF);

    private final int protocolNumber;

    TransportProtocol(int protocolNumber) {
        this.protocolNumber = protocolNumber;
    }

    static TransportProtocol numberToEnum(int protocolNumber) {
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
