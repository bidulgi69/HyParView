package org.example.hyparview.scheduler;

import jakarta.annotation.PreDestroy;
import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.ShuffleRequest;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.protocol.topology.TopologyMessageType;
import org.example.hyparview.queue.TopologyTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ShuffleScheduler {

    private final MembershipService membershipService;
    private final Snowflake snowflake;
    private final HyparViewProperties properties;
    private final TopologyTaskQueue topologyTaskQueue;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture = null;
    private final Logger _logger = LoggerFactory.getLogger(ShuffleScheduler.class);

    @Autowired
    public ShuffleScheduler(MembershipService membershipService,
                            Snowflake snowflake,
                            HyparViewProperties properties,
                            TopologyTaskQueue topologyTaskQueue
    ) {
        this.membershipService = membershipService;
        this.snowflake = snowflake;
        this.properties = properties;
        this.topologyTaskQueue = topologyTaskQueue;
    }

    public void run() {
        _logger.info("[Schedule] Running shuffle request sender...");
        scheduledFuture = executor.scheduleAtFixedRate(
            shuffle(),
            properties.getShuffleInterval(),
            properties.getShuffleInterval(),
            TimeUnit.MILLISECONDS
        );
    }

    private Runnable shuffle() {
        return () -> {
            long messageId = snowflake.nextId();
            Node id = properties.getId();

            List<Member> sampleActiveMembers = membershipService.getRandomActiveMembersLimit(properties.getShuffleActiveNodeCount());
            List<Member> samplePassiveMembers = membershipService.getRandomPassiveMembersLimit(properties.getShuffleNodeCount() - sampleActiveMembers.size());

            Collection<Member> sampleMemberSet = new ArrayList<>(properties.getShuffleNodeCount());
            sampleMemberSet.addAll(sampleActiveMembers);
            sampleMemberSet.addAll(samplePassiveMembers);

            Collection<Node> sampleSet = sampleMemberSet.stream().map(Member::toNode).toList();

            TopologyMessage shuffleRequest = new ShuffleRequest(
                messageId,
                TopologyMessageType.SHUFFLE_REQUEST,
                properties.getDefaultTtl(),
                id,
                sampleSet
            );

            Member randomActiveMember = membershipService.getRandomActiveMember();
            if (randomActiveMember != null) {
                topologyTaskQueue.submit(randomActiveMember.toNode(), shuffleRequest);
            }
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
