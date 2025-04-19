package org.example.hyparview.event;

import org.example.hyparview.Member;

public record MemberViewChangeEvent(
    Member member,
    boolean activeView
) implements Event{
}
