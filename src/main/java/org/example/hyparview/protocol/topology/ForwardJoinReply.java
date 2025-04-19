package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

public class ForwardJoinReply extends TopologyMessage {

    private final Node acceptor;

    public ForwardJoinReply(long messageId,
                            TopologyMessageType type,
                            int ttl,
                            Node acceptor
    ) {
        super(messageId, type, ttl);
        this.acceptor = acceptor;
    }

    public Node getAcceptor() {
        return acceptor;
    }
}
