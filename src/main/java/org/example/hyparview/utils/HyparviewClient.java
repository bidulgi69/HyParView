package org.example.hyparview.utils;

import io.netty.channel.ChannelOption;
import org.example.hyparview.MembershipService;
import org.example.hyparview.event.MemberDisconnectEvent;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.Message;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.gossip.MembershipChangeType;
import org.example.hyparview.protocol.topology.TopologyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Component
public class HyparviewClient {

    private final MembershipService membershipService;
    private final MembershipEventDispatcher dispatcher;

    private final WebClient webClient;
    private final Scheduler downstreamEventLoop;
    private final Logger _logger = LoggerFactory.getLogger(HyparviewClient.class);

    @Autowired
    public HyparviewClient(MembershipService membershipService,
                           MembershipEventDispatcher dispatcher,
                           WebClient.Builder webClientBuilder
    ) {
        this.membershipService = membershipService;
        this.dispatcher = dispatcher;
        this.downstreamEventLoop = Schedulers.newSingle("downstreamEventLoop");
        this.webClient = webClientBuilder
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .disableRetry(true)
                    .keepAlive(true)
                    .responseTimeout(Duration.ofMillis(500L))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
            ))
            .build();
    }

    public Mono<Void> doPost(Node node, Message payload) {
        String url = node.address() + (payload instanceof TopologyMessage ? "/topology" : "/gossip");
        return webClient.post()
            .uri(url)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .subscribeOn(downstreamEventLoop)
            .doOnError(e -> {
                _logger.warn("Failed to send message to {}. Cause: {}", url, e.getMessage());
                membershipService.disconnect(node.nodeId());
                dispatcher.dispatch(new MemberDisconnectEvent(
                    node.toMember(),
                    MembershipChangeType.FAIL
                ));
            })
            .onErrorResume(e -> {
                _logger.error("An error occurred while sending to {}. Cause: {}", url, e.getMessage());
                // ignore error
                return Mono.empty();
            });
    }
}
