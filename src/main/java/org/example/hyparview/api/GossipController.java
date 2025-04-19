package org.example.hyparview.api;

import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.gossip.GossipHandler;
import org.example.hyparview.protocol.gossip.Gossip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GossipController {

    private final HandlerTemplate<Gossip> handler;

    @Autowired
    public GossipController(GossipHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/gossip")
    public ResponseEntity<Void> handleGossip(@RequestBody Gossip gossip) {
        handler.handle(gossip);
        return ResponseEntity.ok().build();
    }
}
