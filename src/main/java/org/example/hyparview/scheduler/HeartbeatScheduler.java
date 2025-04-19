package org.example.hyparview.scheduler;

import jakarta.annotation.PreDestroy;
import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.gossip.GossipMessageType;
import org.example.hyparview.protocol.gossip.Heartbeat;
import org.example.hyparview.utils.HyparviewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class HeartbeatScheduler {

    private final MembershipService membershipService;
    private final HyparviewClient client;
    private final Snowflake snowflake;
    private final HyparViewProperties properties;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture = null;
    private final Logger _logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    @Autowired
    public HeartbeatScheduler(MembershipService membershipService,
                              HyparviewClient client,
                              Snowflake snowflake,
                              HyparViewProperties properties
    ) {
        this.membershipService = membershipService;
        this.client = client;
        this.snowflake = snowflake;
        this.properties = properties;
    }

    public void run() {
        _logger.info("[Schedule] Running heartbeat sender...");
        scheduledFuture = executor.scheduleAtFixedRate(
            heartbeatRunnable(),
            properties.getHeartbeatInterval(),
            properties.getHeartbeatInterval(),
            TimeUnit.MILLISECONDS
        );
    }

    private Runnable heartbeatRunnable() {
        return () -> {
            int fanoutSize = membershipService.getFanoutSize();
            List<Member> activePeers = membershipService.getRandomActiveMembersLimit(fanoutSize);

            long messageId = snowflake.nextId();
            Gossip heartbeat = new Heartbeat(
                messageId,
                GossipMessageType.HEARTBEAT,
                properties.getDefaultTtl(),
                properties.getNodeId()
            );

            activePeers.forEach(peer -> client.doPost(peer.toNode(), heartbeat).subscribe());
        };
    }

    @PreDestroy
    public void destroy() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        executor.shutdown();
    }
}
