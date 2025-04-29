package org.example.hyparview.protocol.plumtree;

import org.example.hyparview.protocol.Node;

import java.util.List;

public class IHave extends PlumTreeMessage {

    private final Node source;
    private final List<Long> rumors;

    public IHave(long messageId,
                 PlumTreeMessageType type,
                 int ttl,
                 Node source,
                 List<Long> rumors
    ) {
        super(messageId, type, ttl);
        this.source = source;
        this.rumors = rumors;
    }

    public Node getSource() {
        return source;
    }

    public List<Long> getRumors() {
        return rumors;
    }
}
