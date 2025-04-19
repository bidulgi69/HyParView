package org.example.hyparview.handler.gossip;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.MessageDeduplicator;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.gossip.Heartbeat;
import org.example.hyparview.protocol.gossip.Membership;
import org.example.hyparview.utils.HyparviewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class GossipHandler extends HandlerTemplate<Gossip> {

    private final MembershipService membershipService;
    private final HyparviewClient client;
    private final Logger _logger = LoggerFactory.getLogger(GossipHandler.class);

    @Autowired
    public GossipHandler(MembershipService membershipService,
                         HyparviewClient client,
                         MessageDeduplicator messageDeduplicator
    ) {
        super(messageDeduplicator);
        this.membershipService = membershipService;
        this.client = client;
    }

    @Override
    protected boolean process(Gossip gossip) {
        _logger.info("Received a gossip({}), type: {} with ttl: {}", gossip.getMessageId(), gossip.getType(), gossip.getTtl());
        switch (gossip.getType()) {
            case HEARTBEAT -> {
                Heartbeat heartbeat = (Heartbeat) gossip;
                Instant requestAt = gossip.getTimestamp();
                membershipService.applyHeartbeat(heartbeat.getSourceId(), requestAt);
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
            peers.forEach(peer -> client.doPost(peer.toNode(), gossip).subscribe());
        }
    }
}
