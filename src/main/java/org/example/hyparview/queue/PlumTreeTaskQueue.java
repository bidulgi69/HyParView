package org.example.hyparview.queue;

import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.plumtree.PlumTreeMessage;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PlumTreeTaskQueue {

    private final HyparviewClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public PlumTreeTaskQueue(HyparviewClient client) {
        this.client = client;
    }

    public void submit(Node peer, PlumTreeMessage message) {
        executor.submit(() -> {
            client.doPost(peer, message).subscribe();
        });
    }
}
