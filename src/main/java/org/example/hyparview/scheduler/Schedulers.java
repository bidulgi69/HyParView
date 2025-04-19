package org.example.hyparview.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Schedulers {

    private final HeartbeatScheduler heartbeatScheduler;
    private final ShuffleScheduler shuffleScheduler;

    @Autowired
    public Schedulers(HeartbeatScheduler heartbeatScheduler, ShuffleScheduler shuffleScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
        this.shuffleScheduler = shuffleScheduler;
    }

    public void run() {
        heartbeatScheduler.run();
        shuffleScheduler.run();
    }

    public void destroyExplicit() {
        heartbeatScheduler.destroy();
        shuffleScheduler.destroy();
    }
}
