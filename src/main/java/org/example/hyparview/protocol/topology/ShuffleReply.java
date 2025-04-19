package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

import java.util.Collection;

public class ShuffleReply extends TopologyMessage {

    private final String originId;
    private final Collection<Node> replySet;

    public ShuffleReply(long messageId,
                        TopologyMessageType type,
                        int ttl,
                        String originId,
                        Collection<Node> replySet
    ) {
        super(messageId, type, ttl);
        this.originId = originId;
        this.replySet = replySet;
    }

    public String getOriginId() {
        return originId;
    }

    public Collection<Node> getReplySet() {
        return replySet;
    }
}
