package org.example.hyparview.protocol;

import org.example.hyparview.Member;

public record Node(
    String nodeId,
    String host,
    int port
) {

    public Member toMember() {
        return new Member(
            nodeId,
            host,
            port
        );
    }

    public String address() {
        return "http://" + host + ":" + port;
    }
}
