package org.example.hyparview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.hyparview.protocol.Node;

import java.time.Instant;

public class Member {
    private final String id;
    private final String host;
    private final int port;
    private Instant lastSeen;   // gossip 에 의해서만 갱신됨.

    public Member(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.lastSeen = Instant.now();
    }

    public String address() {
        return "http://" + host + ":" + port;
    }

    public void updateLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @JsonIgnore
    public Instant getLastSeen() {
        return lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Member)) {
            return false;
        }

        return id.equals(((Member) o).id);
    }

    public Node toNode() {
        return new Node(id, host, port);
    }
}
