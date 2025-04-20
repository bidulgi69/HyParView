package org.example.hyparview.api;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.Disconnect;
import org.example.hyparview.protocol.topology.JoinRequest;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.protocol.topology.TopologyMessageType;
import org.example.hyparview.scheduler.Schedulers;
import org.example.hyparview.utils.HyparviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collection;

@RestController
public class TestController {

    private final MembershipService membershipService;
    private final Snowflake snowflake;
    private final HyparviewClient client;
    private final HyparViewProperties properties;
    private final Schedulers schedulers;
    private final ConfigurableApplicationContext configurableApplicationContext;

    @Autowired
    public TestController(MembershipService membershipService,
                          Snowflake snowflake,
                          HyparviewClient client,
                          HyparViewProperties properties,
                          Schedulers schedulers,
                          ConfigurableApplicationContext configurableApplicationContext
    ) {
        this.membershipService = membershipService;
        this.snowflake = snowflake;
        this.client = client;
        this.properties = properties;
        this.schedulers = schedulers;
        this.configurableApplicationContext = configurableApplicationContext;
    }

    @GetMapping("/view")
    public Collection<Member> view(@RequestParam(name = "region") String region) {
        return "ACTIVE".equalsIgnoreCase(region) ?
            membershipService.getActiveMembers() :
            membershipService.getPassiveMembers();
    }

    @GetMapping("/size-limit")
    public int sizeLimit(@RequestParam(name = "region") String region) {
        return "ACTIVE".equalsIgnoreCase(region) ?
            membershipService.getActiveViewSizeLimit() :
            membershipService.getPassiveViewSizeLimit();
    }

    @PostMapping("/join")
    public void join(@RequestBody Node seed) {
        TopologyMessage join = new JoinRequest(
            snowflake.nextId(),
            TopologyMessageType.JOIN,
            0,
            properties.getId()
        );

        client.doPost(seed, join)
            .then(Mono.defer(() -> {
                membershipService.join(seed.toMember());
                return Mono.empty();
            }))
            .subscribe();
    }

    @GetMapping("/terminate")
    public void terminate() {
        TopologyMessage disconnect = new Disconnect(
            snowflake.nextId(),
            TopologyMessageType.DISCONNECT,
            0,
            properties.getNodeId()
        );

        Member randomActiveMember = membershipService.getRandomActiveMember();
        client.doPost(randomActiveMember.toNode(), disconnect).block();
        schedulers.destroyExplicit();
        // shutdown
        new Thread(() -> {
            int exit = SpringApplication.exit(configurableApplicationContext, () -> 15);
            System.exit(exit);
        }).start();
    }

    @GetMapping("/schedule")
    public void enableSchedule() {
        schedulers.run();
    }
}
