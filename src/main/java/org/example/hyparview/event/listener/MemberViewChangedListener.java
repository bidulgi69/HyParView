package org.example.hyparview.event.listener;

import org.example.hyparview.Snowflake;
import org.example.hyparview.event.Event;
import org.example.hyparview.event.MemberViewChangeEvent;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.topology.NeighborSuggest;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.protocol.topology.TopologyMessageType;
import org.example.hyparview.queue.TopologyTaskQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberViewChangedListener {

    @Autowired
    public MemberViewChangedListener(MembershipEventDispatcher dispatcher,
                                     Snowflake snowflake,
                                     TopologyTaskQueue topologyTaskQueue
    ) {
        dispatcher.registerConsumer(event -> {
            if (!support(event)) {
                return;
            }

            MemberViewChangeEvent evt = (MemberViewChangeEvent) event;
            TopologyMessage neighborSuggest = new NeighborSuggest(
                snowflake.nextId(),
                TopologyMessageType.NEIGHBOR,
                0,
                evt.member().toNode(),
                evt.activeView()
            );

            topologyTaskQueue.submit(evt.member(), neighborSuggest);
        });
    }

    private boolean support(Event event) {
        return event instanceof MemberViewChangeEvent;
    }
}
