package org.example.hyparview.queue;

import org.example.hyparview.Member;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.plumtree.IHave;
import org.example.hyparview.protocol.plumtree.PlumTreeMessage;
import org.example.hyparview.protocol.plumtree.PlumTreeMessageType;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Component
public class BroadcastTaskQueue {

    private final HyparviewClient client;
    private final Snowflake snowflake;
    private final HyparViewProperties properties;
    private final PlumTreeTaskQueue plumTreeTaskQueue;
    private final Scheduler eagerPushScheduler = Schedulers.newSingle("eagerPushExecutor");
    private final Scheduler lazyPushScheduler = Schedulers.newSingle("lazyPushExecutor");

    @Autowired
    public BroadcastTaskQueue(HyparviewClient client,
                              Snowflake snowflake,
                              HyparViewProperties properties,
                              PlumTreeTaskQueue plumTreeTaskQueue
    ) {
        this.client = client;
        this.snowflake = snowflake;
        this.properties = properties;
        this.plumTreeTaskQueue = plumTreeTaskQueue;
    }

    public void submit(Collection<Member> peers, Gossip gossip) {
        boolean isUnicast = gossip.getTtl() == 0;
        peers.forEach(peer -> {
            if (isUnicast || Member.PushType.EAGER == peer.getPushType()) {
                client.doPost(peer.toNode(), gossip)
                    .subscribeOn(eagerPushScheduler)
                    .subscribe();
            } else if (Member.PushType.LAZY == peer.getPushType()) {
                Mono.delay(Duration.ofMillis(properties.getLazyPushDelay()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(lazyPushScheduler)
                    .then(Mono.fromRunnable(() -> {
                        PlumTreeMessage iHave = new IHave(
                            snowflake.nextId(),
                            PlumTreeMessageType.IHAVE,
                            properties.getDefaultTtl(),
                            properties.getId(),
                            List.of(gossip.getMessageId())
                        );
                        plumTreeTaskQueue.submit(peer.toNode(), iHave);
                    }))
                    .subscribe();
            }
        });
    }

    public void submit(Member peer, Gossip gossip) {
        this.submit(List.of(peer), gossip);
    }
}
