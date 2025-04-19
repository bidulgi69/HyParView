package org.example.hyparview;

import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.JoinRequest;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.example.hyparview.protocol.topology.TopologyMessageType;
import org.example.hyparview.scheduler.Schedulers;
import org.example.hyparview.utils.HyparviewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@DependsOn(value = {"snowflakeConfiguration", "hyparViewProperties"})
public class HyparviewBootstrap implements ApplicationRunner {

    private final HyparviewClient client;
    private final Snowflake snowflake;
    private final MembershipService membershipService;
    private final HyparViewProperties properties;
    private final Schedulers schedulers;
    private final Logger _logger = LoggerFactory.getLogger(HyparviewBootstrap.class);

    @Autowired
    public HyparviewBootstrap(HyparviewClient client,
                              Snowflake snowflake,
                              MembershipService membershipService,
                              HyparViewProperties properties,
                              Schedulers schedulers
    ) {
        this.client = client;
        this.snowflake = snowflake;
        this.membershipService = membershipService;
        this.properties = properties;
        this.schedulers = schedulers;
    }

    @Override
    public void run(ApplicationArguments args) {
        String seedProperty = properties.getSeed();
        if (properties.enableBootStrap() && seedProperty != null && !seedProperty.isBlank()) {
            String[] seedNodes = seedProperty.split(",");
            for (String seed : seedNodes) {
                String[] idAndAddress = seed.split("=");
                String[] hostAndPort = idAndAddress[1].split(":");
                _logger.info("Trying to send `joinRequest` to node {}({})", idAndAddress[0], idAndAddress[1]);

                long messageId = snowflake.nextId();
                TopologyMessage joinRequest = new JoinRequest(messageId, TopologyMessageType.JOIN, 0, properties.getId());

                Node seedNode = new Node(idAndAddress[0], hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                client.doPost(seedNode, joinRequest)
                    .then(Mono.defer(() -> {
                        _logger.info("Node({}) accepts join request, so add it to active view.", idAndAddress[0]);
                        membershipService.join(seedNode.toMember());
                        return Mono.empty();
                    }))
                    .subscribe();
            }
        }

        // run schedules
        schedulers.run();
    }
}
