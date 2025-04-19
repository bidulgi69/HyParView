package org.example.hyparview.configuration;

import jakarta.annotation.PostConstruct;
import org.example.hyparview.protocol.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HyparViewProperties {

    @Value("${server.port:8080}")
    private int port;
    @Value("${hyparview.node-id}")
    private String nodeId;
    @Value("${hyparview.seed:}")
    private String seed;
    @Value("${hyparview.expected-network-scale:10}")
    private int networkScale;
    @Value("${hyparview.protocol.default-ttl}")
    private int defaultTtl;
    @Value("${hyparview.protocol.topology.shuffle.node-count:6}")
    private int shuffleNodeCount;
    @Value("${hyparview.protocol.topology.shuffle.active-node-count:2}")
    private int shuffleActiveNodeCount;
    @Value("${hyparview.protocol.topology.shuffle.interval-ms}")
    private int shuffleInterval;
    @Value("${hyparview.protocol.gossip.heartbeat.interval-ms}")
    private int heartbeatInterval;
    @Value("${hyparview.cache.max-entry-size:3000}")
    private int cacheMaxEntrySize;
    @Value("${hyparview.cache.retention-ms}")
    private int cacheRetention;
    @Value("${hyparview.cache.clean-init-delay:30000}")
    private int cacheCleanInitDelay;
    @Value("${hyparview.snowflake.custom-epoch}")
    private long customEpoch;

    private Node id;

    @PostConstruct
    public void init() {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalStateException("property:nodeId shouldn't be null or blank.");
        }

        id = new Node(nodeId, nodeId, port);
    }

    public Node getId() {
        return id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getSeed() {
        return seed;
    }

    public int getNetworkScale() {
        return networkScale;
    }

    public int getDefaultTtl() {
        return defaultTtl;
    }

    public int getShuffleNodeCount() {
        return shuffleNodeCount;
    }

    public int getShuffleActiveNodeCount() {
        return shuffleActiveNodeCount;
    }

    public int getShuffleInterval() {
        return shuffleInterval;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public int getCacheMaxEntrySize() {
        return cacheMaxEntrySize;
    }

    public int getCacheRetention() {
        return cacheRetention;
    }

    public int getCacheCleanInitDelay() {
        return cacheCleanInitDelay;
    }

    public long getCustomEpoch() {
        return customEpoch;
    }
}
