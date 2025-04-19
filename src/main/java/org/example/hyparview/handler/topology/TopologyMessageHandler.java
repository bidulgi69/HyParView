package org.example.hyparview.handler.topology;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.MessageDeduplicator;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.*;
import org.example.hyparview.utils.HyparviewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class TopologyMessageHandler extends HandlerTemplate<TopologyMessage> {

    private final MembershipService membershipService;
    private final HyparviewClient client;
    private final HyparViewProperties properties;
    private final Snowflake snowflake;
    private final Logger _logger = LoggerFactory.getLogger(TopologyMessageHandler.class);

    @Autowired
    public TopologyMessageHandler(MembershipService membershipService,
                                  HyparviewClient client,
                                  HyparViewProperties properties,
                                  MessageDeduplicator messageDeduplicator,
                                  Snowflake snowflake
    ) {
        super(messageDeduplicator);
        this.membershipService = membershipService;
        this.client = client;
        this.properties = properties;
        this.snowflake = snowflake;
    }

    @Override
    protected boolean process(TopologyMessage message) {
        int ttl = message.getTtl();
        _logger.info("Received a topology message({}), type: {} with ttl: {}", message.getMessageId(), message.getType(), message.getTtl());

        // if ttl > 0, just forward the message to random peer in active view
        if (ttl > 0) {
            message.decrementTtl();
            Member randomActiveMember = membershipService.getRandomActiveMember();
            if (randomActiveMember != null) {
                _logger.info("Forward request to random active peer {}", randomActiveMember.getId());
                client.doPost(randomActiveMember.toNode(), message).subscribe();
            }
            return false;
        }

        switch (message.getType()) {
            case JOIN -> {
                JoinRequest joinRequest = (JoinRequest) message;
                TopologyMessage forwardJoinRequest = new ForwardJoinRequest(
                    snowflake.nextId(),
                    TopologyMessageType.FWD_JOIN,
                    properties.getDefaultTtl(),
                    joinRequest.getNode(),
                    joinRequest.getNode()
                );

                Member randomActiveMember = membershipService.getRandomActiveMember();
                if (randomActiveMember != null) {
                    client.doPost(randomActiveMember.toNode(), forwardJoinRequest).subscribe();
                }
                membershipService.forceJoin(joinRequest.getNode().toMember());
            }
            case FWD_JOIN -> {
                ForwardJoinRequest forwardJoinRequest = (ForwardJoinRequest) message;
                Node originNode = forwardJoinRequest.getNode();
                // activeView 에 자리가 없다면 랜덤한 노드를 passive view 로 이동시킴
                membershipService.forceJoin(originNode.toMember());

                Member originMember = originNode.toMember();
                TopologyMessage forwardJoinReply = new ForwardJoinReply(
                    snowflake.nextId(),
                    TopologyMessageType.FWD_JOIN_REPLY,
                    0,
                    properties.getId()
                );
                client.doPost(originMember.toNode(), forwardJoinReply).subscribe();
            }
            case FWD_JOIN_REPLY -> {
                ForwardJoinReply forwardJoinReply = (ForwardJoinReply) message;
                Node acceptor = forwardJoinReply.getAcceptor();
                membershipService.forceJoin(acceptor.toMember());
            }
            case SHUFFLE_REQUEST -> {
                ShuffleRequest shuffleRequest = (ShuffleRequest) message;
                Collection<Node> sampleSet = shuffleRequest.getSampleSet();
                Collection<Member> sampleMemberSet = sampleSet.stream()
                    .map(Node::toMember)
                    .toList();
                membershipService.mergeIntoActiveView(sampleMemberSet);

                List<Node> replySet = membershipService.getRandomPassiveMembersLimit(properties.getShuffleNodeCount())
                    .stream()
                    .map(member -> new Node(member.getId(), member.getHost(), member.getPort()))
                    .toList();
                TopologyMessage shuffleReply = new ShuffleReply(
                    snowflake.nextId(),
                    TopologyMessageType.SHUFFLE_REPLY,
                    0,
                    shuffleRequest.getOriginNode().nodeId(),
                    replySet
                );

                client.doPost(shuffleRequest.getOriginNode(), shuffleReply).subscribe();
            }
            case SHUFFLE_REPLY -> {
                ShuffleReply shuffleReply = (ShuffleReply) message;
                Collection<Node> replySet = shuffleReply.getReplySet();
                Collection<Member> replyMemberSet = replySet.stream()
                    .map(Node::toMember)
                    .toList();
                membershipService.mergeIntoPassiveView(replyMemberSet);
            }
            case NEIGHBOR -> {
                NeighborSuggest neighborSuggest = (NeighborSuggest) message;
                Node node = neighborSuggest.getNode();
                membershipService.joinIfAvailable(node.toMember());
            }
            case DISCONNECT -> {
                Disconnect disconnect = (Disconnect) message;
                membershipService.disconnect(disconnect.getNodeId());
            }
        }
        return true;
    }
}
