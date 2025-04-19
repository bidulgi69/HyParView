package org.example.hyparview.protocol.gossip;

public enum GossipMessageType {

    HEARTBEAT,
    MEMBERSHIP
}

/*
HEARTBEAT | A → B | nodeId, timestamp, ttl | Liveness 확인용, ttl로 전파 범위 제한
MEMBERSHIP (Gossip) | A → * | action, nodeId, timestamp, ttl | JOIN/LEAVE/FAIL 이벤트를 전체 네트워크에 전파
 */