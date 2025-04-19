package org.example.hyparview.event;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class MembershipEventDispatcher {

    private final List<Consumer<Event>> eventConsumers = new LinkedList<>();

    public void registerConsumer(Consumer<Event> consumer) {
        eventConsumers.add(consumer);
    }

    public void dispatch(Event event) {
        eventConsumers.forEach(consumer -> consumer.accept(event));
    }
}