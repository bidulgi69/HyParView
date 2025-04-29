package org.example.hyparview.handler.plumtree;

import org.example.hyparview.Member;
import org.example.hyparview.Member.PushType;
import org.example.hyparview.MembershipService;
import org.example.hyparview.Snowflake;
import org.example.hyparview.handler.HandlerTemplate;
import org.example.hyparview.handler.MessageDeduplicator;
import org.example.hyparview.protocol.Message;
import org.example.hyparview.protocol.gossip.Gossip;
import org.example.hyparview.protocol.plumtree.*;
import org.example.hyparview.queue.BroadcastTaskQueue;
import org.example.hyparview.queue.PlumTreeTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlumTreeMessageHandler extends HandlerTemplate<PlumTreeMessage> {

    private final MembershipService membershipService;
    private final Snowflake snowflake;
    private final PlumTreeTaskQueue plumTreeTaskQueue;
    private final BroadcastTaskQueue broadcastTaskQueue;
    private final Logger _logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public PlumTreeMessageHandler(MessageDeduplicator messageDeduplicator,
                                  MembershipService membershipService,
                                  Snowflake snowflake,
                                  PlumTreeTaskQueue plumTreeTaskQueue,
                                  BroadcastTaskQueue broadcastTaskQueue
    ) {
        super(messageDeduplicator);
        this.membershipService = membershipService;
        this.snowflake = snowflake;
        this.plumTreeTaskQueue = plumTreeTaskQueue;
        this.broadcastTaskQueue = broadcastTaskQueue;
    }

    @Override
    protected boolean process(PlumTreeMessage message) {
        _logger.info("Received a plumTree message({}), type: {} with ttl: {}", message.getMessageId(), message.getType(), message.getTtl());
        switch (message.getType()) {
            case PRUNE -> {
                Prune prune = (Prune) message;
                membershipService.updateActiveMemberPushType(prune.getSourceId(), PushType.LAZY);
            }
            case IHAVE -> {
                IHave ihave = (IHave) message;
                List<Long> missingRumorIds = new ArrayList<>();
                for (long rumorId : ihave.getRumors()) {
                    Message msg = find(rumorId);
                    if (msg == null) {
                        missingRumorIds.add(rumorId);
                    }
                }
                // send graft
                for (long missingRumorId : missingRumorIds) {
                    PlumTreeMessage graft = new Graft(
                        snowflake.nextId(),
                        PlumTreeMessageType.GRAFT,
                        0,
                        ihave.getSource(),
                        missingRumorId
                    );
                    plumTreeTaskQueue.submit(ihave.getSource(), graft);
                }
            }
            case GRAFT -> {
                Graft graft = (Graft) message;
                Message rumor = find(graft.getRequestRumorId());
                if (rumor instanceof Gossip) {
                    Member requester = membershipService.updateActiveMemberPushType(graft.getRequester().nodeId(), PushType.EAGER);
                    if (requester != null) {
                        broadcastTaskQueue.submit(requester, (Gossip)rumor);
                    }
                } else {
                    // if there's no cached message
                    // e.g. respond nack
                }
            }
        }
        return true;
    }
}
