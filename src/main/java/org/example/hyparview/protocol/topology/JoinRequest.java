package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

public class JoinRequest extends TopologyMessage {

    private final Node node;

    public JoinRequest(long messageId,
                       TopologyMessageType type,
                       int ttl,
                       Node node
    ) {
        super(messageId, type, ttl);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}

// JOIN 을 수신한 노드는
// FWD_JOIN 메세지를 생성해서 다음 random active peer 에게 전송