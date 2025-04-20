package org.example.hyparview.event;

import org.example.hyparview.Member;

public record MemberJoinEvent(
    Member member
) implements Event {
}
