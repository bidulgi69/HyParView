package org.example.hyparview.protocol.plumtree;

import org.example.hyparview.protocol.Node;

public class Graft extends PlumTreeMessage {

    private final Node requester;
    private final long requestRumorId;

    public Graft(long messageId,
                 PlumTreeMessageType type,
                 int ttl,
                 Node requester,
                 long requestRumorId
    ) {
        super(messageId, type, ttl);
        this.requester = requester;
        this.requestRumorId = requestRumorId;
    }

    public Node getRequester() {
        return requester;
    }

    public long getRequestRumorId() {
        return requestRumorId;
    }
}
