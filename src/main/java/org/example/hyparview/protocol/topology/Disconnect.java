package org.example.hyparview.protocol.topology;

public class Disconnect extends TopologyMessage {

    private final String nodeId;

    public Disconnect(long messageId, TopologyMessageType type, int ttl, String nodeId) {
        super(messageId, type, ttl);
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}

// nodeId 를 active, passive view 에서 제외한 뒤
// gossip 을 통해 broadcasting
// gossip 의 경우 active view 전부에게 전송
