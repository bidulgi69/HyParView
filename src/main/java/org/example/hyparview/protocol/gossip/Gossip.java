package org.example.hyparview.protocol.gossip;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.hyparview.protocol.Message;

import java.time.Instant;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Heartbeat.class, name = "HEARTBEAT"),
    @JsonSubTypes.Type(value = Membership.class, name = "MEMBERSHIP")
})
public abstract class Gossip implements Message {

    private final long messageId;
    private final GossipMessageType type;
    private int ttl;
    private final Instant timestamp;

    public Gossip(long messageId, GossipMessageType type, int ttl) {
        this.messageId = messageId;
        this.type = type;
        this.ttl = ttl;
        this.timestamp = Instant.now();
    }

    public long getMessageId() {
        return messageId;
    }

    public GossipMessageType getType() {
        return type;
    }

    public int getTtl() {
        return ttl;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void decrementTtl() {
        ttl = ttl - 1;
    }
}
