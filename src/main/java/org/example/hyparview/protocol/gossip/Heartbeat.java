package org.example.hyparview.protocol.gossip;

import org.example.hyparview.protocol.Node;

public class Heartbeat extends Gossip {

    public Heartbeat(long messageId,
                     GossipMessageType type,
                     int ttl,
                     Node source
    ) {
        super(messageId, type, ttl, source);
    }
}
