package org.example.hyparview.protocol.plumtree;

public class Prune extends PlumTreeMessage {

    private final String sourceId;

    public Prune(long messageId,
                 PlumTreeMessageType type,
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
