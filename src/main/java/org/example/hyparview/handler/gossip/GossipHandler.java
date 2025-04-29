package org.example.hyparview.handler.gossip;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.MessageDeduplicator;
import org.example.hyparview.protocol.Message;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.gossip.Heartbeat;
import org.example.hyparview.protocol.gossip.Membership;
import org.example.hyparview.protocol.plumtree.PlumTreeMessage;
import org.example.hyparview.protocol.plumtree.PlumTreeMessageType;
import org.example.hyparview.protocol.plumtree.Prune;
import org.example.hyparview.queue.BroadcastTaskQueue;
import org.example.hyparview.queue.PlumTreeTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class GossipHandler extends HandlerTemplate<Gossip> {

    private final MembershipService membershipService;
    private final Snowflake snowflake;
    private final HyparViewProperties properties;
    private final BroadcastTaskQueue broadcastTaskQueue;
    private final PlumTreeTaskQueue plumTreeTaskQueue;
    private final Logger _logger = LoggerFactory.getLogger(GossipHandler.class);

    @Autowired
    public GossipHandler(MembershipService membershipService,
                         MessageDeduplicator messageDeduplicator,
                         Snowflake snowflake,
                         HyparViewProperties properties,
                         BroadcastTaskQueue broadcastTaskQueue,
                         PlumTreeTaskQueue plumTreeTaskQueue
    ) {
        super(messageDeduplicator);
        this.membershipService = membershipService;
        this.snowflake = snowflake;
        this.properties = properties;
        this.broadcastTaskQueue = broadcastTaskQueue;
        this.plumTreeTaskQueue = plumTreeTaskQueue;
    }

    @Override
    public void handle(Gossip gossip) {
        Message message = find(gossip.getMessageId());
        if (message instanceof Gossip ex) {
            // 이미 동일한 메세지를 다른 노드로 부터 수신한 경우
            if (!ex.getSource().nodeId().equals(gossip.getSource().nodeId())) {
                PlumTreeMessage prune = new Prune(
                    snowflake.nextId(),
                    PlumTreeMessageType.PRUNE,
                    0,
                    properties.getNodeId()
                );
                plumTreeTaskQueue.submit(gossip.getSource(), prune);
                return;
            }
        }

        // handle message
        boolean _processed = process(gossip);
        if (_processed) {
            super.put(gossip);
        }
    }

    @Override
    protected boolean process(Gossip gossip) {
        _logger.info("Received a gossip({}), type: {} with ttl: {}", gossip.getMessageId(), gossip.getType(), gossip.getTtl());
        switch (gossip.getType()) {
            case HEARTBEAT -> {
                Heartbeat heartbeat = (Heartbeat) gossip;
                Instant requestAt = gossip.getTimestamp();
                membershipService.applyHeartbeat(heartbeat.getSource().nodeId(), requestAt);
            }
            case MEMBERSHIP -> {
                Membership membership = (Membership) gossip;
                Node node = membership.getNode();
                _logger.info("Received membership change gossip, type: {} with changed node: {}", membership.getChangeType(), node);
                switch (membership.getChangeType()) {
                    case JOIN -> membershipService.mergeIntoPassiveView(List.of(node.toMember()));
                    case LEAVE, FAIL -> membershipService.disconnect(node.nodeId());
                }
            }
        }
        // fanout gossip
        fanout(gossip);
        return true;
    }

    private void fanout(Gossip gossip) {
        if (gossip.getTtl() > 0) {
            gossip.decrementTtl();
            int fanout = membershipService.getFanoutSize();
            List<Member> peers = membershipService.getRandomActiveMembersLimit(fanout);
            broadcastTaskQueue.submit(peers, gossip);
        }
    }
}
