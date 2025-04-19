package org.example.hyparview.protocol.topology;

import org.example.hyparview.protocol.Node;

import java.util.Collection;

public class ShuffleRequest extends TopologyMessage {

    private final Node originNode;
    private final Collection<Node> sampleSet;

    public ShuffleRequest(long messageId,
                          TopologyMessageType type,
                          int ttl,
                          Node originNode,
                          Collection<Node> sampleSet
    ) {
        super(messageId, type, ttl);
        this.originNode = originNode;
        this.sampleSet = sampleSet;
    }

    public Node getOriginNode() {
        return originNode;
    }

    public Collection<Node> getSampleSet() {
        return sampleSet;
    }
}
