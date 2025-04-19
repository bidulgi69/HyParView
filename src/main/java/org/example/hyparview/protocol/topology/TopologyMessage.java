package org.example.hyparview.protocol.topology;

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
    @JsonSubTypes.Type(value = JoinRequest.class, name = "JOIN"),
    @JsonSubTypes.Type(value = ForwardJoinRequest.class, name = "FWD_JOIN"),
    @JsonSubTypes.Type(value = ForwardJoinReply.class, name = "FWD_JOIN_REPLY"),
    @JsonSubTypes.Type(value = ShuffleRequest.class, name = "SHUFFLE_REQUEST"),
    @JsonSubTypes.Type(value = ShuffleReply.class, name = "SHUFFLE_REPLY"),
    @JsonSubTypes.Type(value = NeighborSuggest.class, name = "NEIGHBOR"),
    @JsonSubTypes.Type(value = Disconnect.class, name = "DISCONNECT"),
})
// topology message 는 1명의 peer 에게만 forward 한다.
public abstract class TopologyMessage implements Message {

    private final long messageId; // snowflakeId
    private final TopologyMessageType type;
    private int ttl;
    private final Instant timestamp;

    public TopologyMessage(long messageId, TopologyMessageType type, int ttl) {
        this.messageId = messageId;
        this.type = type;
        this.ttl = ttl;
        this.timestamp = Instant.now();
    }

    public long getMessageId() {
        return messageId;
    }

    public TopologyMessageType getType() {
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
