package org.example.hyparview.event.listener;

import org.example.hyparview.Snowflake;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.NeighborSuggest;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.protocol.topology.TopologyMessageType;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberViewChangedListener {

    @Autowired
    public MemberViewChangedListener(MembershipEventDispatcher dispatcher,
                                     Snowflake snowflake,
                                     HyparviewClient client
    ) {
        dispatcher.registerConsumer(event -> {
            Node node = event.member().toNode();
            TopologyMessage neighborSuggest = new NeighborSuggest(
                snowflake.nextId(),
                TopologyMessageType.NEIGHBOR,
                0,
                node,
                event.activeView()
            );

            client.doPost(node, neighborSuggest).subscribe();
        });
    }
}
