package org.example.hyparview.protocol.gossip;

import org.example.hyparview.protocol.Node;

public class Membership extends Gossip {

    private final MembershipChangeType changeType;
    private final Node node;

    public Membership(long messageId,
                      GossipMessageType type,
                      int ttl,
                      MembershipChangeType changeType,
                      Node node
    ) {
        super(messageId, type, ttl);
        this.changeType = changeType;
        this.node = node;
    }

    public MembershipChangeType getChangeType() {
        return changeType;
    }

    public Node getNode() {
        return node;
    }
}
