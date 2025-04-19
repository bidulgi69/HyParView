package org.example.hyparview.protocol.gossip;

public class Heartbeat extends Gossip {

    private final String sourceId;

    public Heartbeat(long messageId,
                     GossipMessageType type,
                     int ttl,
                     String sourceId
    ) {
        super(messageId, type, ttl);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }
}
