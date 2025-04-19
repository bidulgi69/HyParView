package org.example.hyparview.event;

import org.example.hyparview.Member;
import org.example.hyparview.protocol.gossip.MembershipChangeType;

public record MemberDisconnectEvent(
    Member member,
    MembershipChangeType reason
) implements Event{
}
