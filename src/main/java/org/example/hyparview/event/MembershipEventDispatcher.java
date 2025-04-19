package org.example.hyparview.event;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class MembershipEventDispatcher {

    private final List<Consumer<MemberViewChangeEvent>> viewChangeConsumers = new LinkedList<>();

    public void registerConsumer(Consumer<MemberViewChangeEvent> consumer) {
        viewChangeConsumers.add(consumer);
    }

    public void dispatch(MemberViewChangeEvent event) {
        viewChangeConsumers.forEach(consumer -> consumer.accept(event));
    }
}