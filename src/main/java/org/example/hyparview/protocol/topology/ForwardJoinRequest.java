package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

public class ForwardJoinRequest extends TopologyMessage {

    // FWD_JOIN 을 처음으로 전파한 노드
    private final Node source;
    // active view 에 추가 요청한 노드
    private final Node node;

    public ForwardJoinRequest(long messageId,
                              TopologyMessageType type,
                              int ttl,
                              Node source,
                              Node node
    ) {
        super(messageId, type, ttl);
        this.source = source;
        this.node = node;
    }

    public Node getSource() {
        return source;
    }

    public Node getNode() {
        return node;
    }
}

// FWD_JOIN 을 수신한 노드는 TTL > 0 인 경우 forward
// TTL=0 인 경우 node 에게 NEIGHBOR(active=true) 를 응답