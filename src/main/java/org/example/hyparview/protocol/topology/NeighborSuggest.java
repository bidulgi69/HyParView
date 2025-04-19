package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

// fwd_join_reply, shuffle_reply 대용으로 사용될 수 있는 유형
// reply 보다 완곡한 표현
public class NeighborSuggest extends TopologyMessage {

    private final Node node;
    // active=true 인 경우 active view 에 추가 요청
    // active=false 인 경우 passive view 에 추가 요청
    private final boolean activeView;

    public NeighborSuggest(long messageId,
                           TopologyMessageType type,
                           int ttl,
                           Node node,
                           boolean activeView
    ) {
        super(messageId, type, ttl);
        this.node = node;
        this.activeView = activeView;
    }

    public Node getNode() {
        return node;
    }

    public boolean isActiveView() {
        return activeView;
    }
}
