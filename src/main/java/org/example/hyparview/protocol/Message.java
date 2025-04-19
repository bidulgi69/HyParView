package org.example.hyparview.protocol;

import java.time.Instant;

public interface Message {
    
    long getMessageId();
    int getTtl();
    Instant getTimestamp();
}
