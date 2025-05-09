package org.example.hyparview.handler.topology;

import org.example.hyparview.Member;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.MessageDeduplicator;
import org.example.hyparview.protocol.Node;
import org.example.hyparview.protocol.topology.*;
import org.example.hyparview.queue.TopologyTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class TopologyMessageHandler extends HandlerTemplate<TopologyMessage> {

    private final MembershipService membershipService;
    private final HyparViewProperties properties;
    private final Snowflake snowflake;
    private final TopologyTaskQueue topologyTaskQueue;
    private final Logger _logger = LoggerFactory.getLogger(TopologyMessageHandler.class);

    @Autowired
    public TopologyMessageHandler(MembershipService membershipService,
                                  HyparViewProperties properties,
                                  MessageDeduplicator messageDeduplicator,
                                  Snowflake snowflake,
                                  TopologyTaskQueue topologyTaskQueue
    ) {
        super(messageDeduplicator);
        this.membershipService = membershipService;
        this.properties = properties;
        this.snowflake = snowflake;
        this.topologyTaskQueue = topologyTaskQueue;
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
                topologyTaskQueue.submit(randomActiveMember, message);
            }
            return false;
        }

        switch (message.getType()) {
            case JOIN -> {
                JoinRequest joinRequest = (JoinRequest) message;
                _logger.info("Handle join request from node {}", joinRequest.getNode().nodeId());
                TopologyMessage forwardJoinRequest = new ForwardJoinRequest(
                    snowflake.nextId(),
                    TopologyMessageType.FWD_JOIN,
                    properties.getDefaultTtl(),
                    joinRequest.getNode(),
                    joinRequest.getNode()
                );

                Member randomActiveMember = membershipService.getRandomActiveMember();
                if (randomActiveMember != null) {
                    topologyTaskQueue.submit(randomActiveMember, forwardJoinRequest);
                }
                membershipService.forceJoin(joinRequest.getNode().toMember());
            }
            case FWD_JOIN -> {
                ForwardJoinRequest forwardJoinRequest = (ForwardJoinRequest) message;
                Node originNode = forwardJoinRequest.getNode();
                _logger.info("Handle forward-join request from origin node {}", originNode.nodeId());
                // activeView 에 자리가 없다면 랜덤한 노드를 passive view 로 이동시킴
                membershipService.forceJoin(originNode.toMember());

                Member originMember = originNode.toMember();
                TopologyMessage forwardJoinReply = new ForwardJoinReply(
                    snowflake.nextId(),
                    TopologyMessageType.FWD_JOIN_REPLY,
                    0,
                    properties.getId()
                );
                topologyTaskQueue.submit(originMember, forwardJoinReply);
            }
            case FWD_JOIN_REPLY -> {
                ForwardJoinReply forwardJoinReply = (ForwardJoinReply) message;
                Node acceptor = forwardJoinReply.getAcceptor();
                _logger.info("Handle forward-join reply from acceptor node {}", acceptor.nodeId());
                membershipService.forceJoin(acceptor.toMember());
            }
            case SHUFFLE_REQUEST -> {
                ShuffleRequest shuffleRequest = (ShuffleRequest) message;
                Collection<Node> sampleSet = shuffleRequest.getSampleSet();
                _logger.info("Handle shuffle request from sample set {}", sampleSet);
                Collection<Member> sampleMemberSet = sampleSet.stream()
                    .map(Node::toMember)
                    .toList();
                membershipService.mergeIntoActiveView(sampleMemberSet);

                List<Node> replySet = membershipService.getRandomPassiveMembersLimit(properties.getShuffleNodeCount())
                    .stream()
                    .map(Member::toNode)
                    .toList();
                TopologyMessage shuffleReply = new ShuffleReply(
                    snowflake.nextId(),
                    TopologyMessageType.SHUFFLE_REPLY,
                    0,
                    shuffleRequest.getOriginNode().nodeId(),
                    replySet
                );

                topologyTaskQueue.submit(shuffleRequest.getOriginNode(), shuffleReply);
            }
            case SHUFFLE_REPLY -> {
                ShuffleReply shuffleReply = (ShuffleReply) message;
                Collection<Node> replySet = shuffleReply.getReplySet();
                _logger.info("Handle shuffle reply from reply set {}", replySet);
                Collection<Member> replyMemberSet = replySet.stream()
                    .map(Node::toMember)
                    .toList();
                membershipService.mergeIntoPassiveView(replyMemberSet);
            }
            case NEIGHBOR -> {
                NeighborSuggest neighborSuggest = (NeighborSuggest) message;
                Node node = neighborSuggest.getNode();
                _logger.info("Handle neighbor suggest from node {} to add {} view", node.nodeId(), neighborSuggest.isActiveView() ? "active" : "passive");
                membershipService.joinIfAvailable(node.toMember(), neighborSuggest.isActiveView());
            }
            case DISCONNECT -> {
                Disconnect disconnect = (Disconnect) message;
                _logger.info("Handle disconnect node {}", disconnect.getNodeId());
                membershipService.disconnect(disconnect.getNodeId());
            }
        }
        return true;
    }
}
