package org.example.hyparview.handler;

import org.example.hyparview.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HandlerTemplate<T extends Message> {

    private final MessageDeduplicator messageDeduplicator;
    private final Logger _logger = LoggerFactory.getLogger(HandlerTemplate.class.getName());

    public HandlerTemplate(MessageDeduplicator messageDeduplicator) {
        this.messageDeduplicator = messageDeduplicator;
    }

    // template method
    public void handle(T message) {
        if (messageDeduplicator.has(message.getMessageId())) {
            _logger.info("Receives duplicate message({})", message.getMessageId());
            return;
        }
        // handle message
        boolean _processed = process(message);
        if (_processed) {
            messageDeduplicator.put(message.getMessageId(), message);
        }
    }

    protected abstract boolean process(T message);

    protected void put(T message) {
        messageDeduplicator.put(message.getMessageId(), message);
    }

    protected Message find(long messageId) {
        return messageDeduplicator.find(messageId);
    }
}
