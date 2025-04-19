package org.example.hyparview.handler;

import jakarta.annotation.PreDestroy;
import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MessageDeduplicator {

    private final HyparViewProperties properties;

    // Because we use a snowflakeId, we can determine the eldest entry based on the id.
    private final NavigableMap<Long, Message> lruCache;
    private final ReentrantLock lock = new ReentrantLock();
    private final Logger _logger = LoggerFactory.getLogger(MessageDeduplicator.class);
    private final ExecutorService cleanerThreadPool = Executors.newSingleThreadExecutor();

    @Autowired
    public MessageDeduplicator(HyparViewProperties properties) {
        this.properties = properties;
        lruCache = new TreeMap<>();
    }

    public boolean has(long messageId) {
        return lruCache.containsKey(messageId);
    }

    public void put(long messageId, Message message) {
        lock.lock();
        try {
            lruCache.put(messageId, message);
            if (lruCache.size() > properties.getCacheMaxEntrySize()) {
                cleanerThreadPool.submit(this::removeEldestEntries);
                _logger.info("Scheduled cache removal task(`removeEldestEntries`).");
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeEldestEntries() {
        _logger.info("Removing eldest cache entries...");
        int retention = properties.getCacheRetention();
        long head = (Instant.now().toEpochMilli() - properties.getCustomEpoch() - retention) << 22;
        SortedMap<Long, Message> headMap = lruCache.headMap(head); // find lower-bound entries
        headMap.keySet().forEach(lruCache::remove);
    }

    @PreDestroy
    public void destroy() {
        cleanerThreadPool.shutdown();
    }
}
