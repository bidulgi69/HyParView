package org.example.hyparview.event.listener;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.event.Event;
import org.example.hyparview.event.MemberDisconnectEvent;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.gossip.GossipMessageType;
import org.example.hyparview.protocol.gossip.Membership;
import org.example.hyparview.queue.BroadcastTaskQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberDisconnectListener {

    @Autowired
    public MemberDisconnectListener(MembershipEventDispatcher dispatcher,
                                    MembershipService membershipService,
                                    HyparViewProperties properties,
                                    Snowflake snowflake,
                                    BroadcastTaskQueue broadcastTaskQueue
    ) {
        dispatcher.registerConsumer(event -> {
            if (!support(event)) {
                return;
            }

            MemberDisconnectEvent evt = (MemberDisconnectEvent) event;
            Node node = evt.member().toNode();
            Gossip gossip = new Membership(
                snowflake.nextId(),
                GossipMessageType.MEMBERSHIP,
                0,
                properties.getId(),
                evt.reason(),
                node
            );

            int fanoutSize = membershipService.getFanoutSize();
            List<Member> activePeers = membershipService.getRandomActiveMembersLimit(fanoutSize);
            broadcastTaskQueue.submit(activePeers, gossip);
        });
    }

    private boolean support(Event event) {
        return event instanceof MemberDisconnectEvent;
    }
}
