package org.example.hyparview.event.listener;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.event.Event;
import org.example.hyparview.event.MemberJoinEvent;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.gossip.GossipMessageType;
import org.example.hyparview.protocol.gossip.Membership;
import org.example.hyparview.protocol.gossip.MembershipChangeType;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberJoinListener {

    @Autowired
    public MemberJoinListener(MembershipEventDispatcher dispatcher,
                              MembershipService membershipService,
                              HyparViewProperties properties,
                              Snowflake snowflake,
                              HyparviewClient client
    ) {
        dispatcher.registerConsumer(event -> {
            if (!support(event)) {
                return;
            }

            MemberJoinEvent evt = (MemberJoinEvent) event;
            Node node = evt.member().toNode();
            Gossip gossip = new Membership(
                snowflake.nextId(),
                GossipMessageType.MEMBERSHIP,
                properties.getDefaultTtl(),
                MembershipChangeType.JOIN,
                node
            );

            int fanoutSize = membershipService.getFanoutSize();
            List<Member> activePeers = membershipService.getRandomActiveMembersLimit(fanoutSize);
            activePeers.forEach(peer -> client.doPost(peer.toNode(), gossip).subscribe());
        });
    }

    private boolean support(Event event) {
        return event instanceof MemberJoinEvent;
    }
}
