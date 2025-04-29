package org.example.hyparview.protocol.plumtree;

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
    @JsonSubTypes.Type(value = Prune.class, name = "PRUNE"),
    @JsonSubTypes.Type(value = IHave.class, name = "IHAVE"),
    @JsonSubTypes.Type(value = Graft.class, name = "GRAFT"),
})
public abstract class PlumTreeMessage implements Message {

    private final long messageId; // snowflakeId
    private final PlumTreeMessageType type;
    private int ttl;
    private final Instant timestamp;

    public PlumTreeMessage(long messageId, PlumTreeMessageType type, int ttl) {
        this.messageId = messageId;
        this.type = type;
        this.ttl = ttl;
        this.timestamp = Instant.now();
    }

    public long getMessageId() {
        return messageId;
    }

    public PlumTreeMessageType getType() {
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
