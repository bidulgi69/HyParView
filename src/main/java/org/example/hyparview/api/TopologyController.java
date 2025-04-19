package org.example.hyparview.api;

import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.topology.TopologyMessageHandler;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class TopologyController {

    private final HandlerTemplate<TopologyMessage> handler;

    @Autowired
    public TopologyController(TopologyMessageHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/topology")
    public ResponseEntity<Void> handleTopologyMessage(@RequestBody TopologyMessage message) {
        handler.handle(message);
        return ResponseEntity.ok().build();
    }
}
