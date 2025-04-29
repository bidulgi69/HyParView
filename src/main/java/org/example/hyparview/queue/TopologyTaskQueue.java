package org.example.hyparview.queue;

import org.example.hyparview.Member;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TopologyTaskQueue {

    private final HyparviewClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public TopologyTaskQueue(HyparviewClient client) {
        this.client = client;
    }

    public void submit(Member peer, TopologyMessage message) {
        executor.submit(() -> {
            client.doPost(peer.toNode(), message).subscribe();
        });
    }

    public void submit(Node peer, TopologyMessage message) {
        executor.submit(() -> {
            client.doPost(peer, message).subscribe();
        });
    }

    public void submit(Node peer, TopologyMessage message, Mono<?> callback) {
        executor.submit(() -> {
            client.doPost(peer, message)
                .then(callback)
                .subscribe();
        });
    }
}
