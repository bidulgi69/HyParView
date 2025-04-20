# HyParView

A lightweight Java implementation of the [HyParView](https://asc.di.fct.unl.pt/~jleitao/pdf/dsn07-leitao.pdf?ref=bartoszsypytkowski.com) membership protocol for peer-to-peer networks. It provides efficient, random peer sampling and reliable membership management.

---

## Key Characteristics

- **TTL-based Forwarding**: Uses a Time-To-Live (TTL) field on topology messages to limit hop counts and prevent broadcast flooding.
- **Partial Membership**: Maintains only a small **active view** and **passive view** per node, avoiding the overhead of tracking all peers—ideal for large-scale clusters.
- **Random Peer Sampling**: Leverages shuffle and gossip subprotocols to continuously mix views, providing uniform random sampling of peers.
- **Localized Failure Detection**: Heartbeat-based liveness checks detect node failures quickly, minimizing impact on the network.
- **Decoupled Layers**: Separates **topology management** (JOIN, SHUFFLE, NEIGHBOR) from **event propagation** (Gossip), ensuring modularity and resilience.

---

## Features

- **JOIN / FWD\_JOIN**: Bootstrap new nodes into the active view and relay join requests using TTL-based forwarding.
- **Shuffle**: Periodically mix active and passive views to maintain random peer sampling.
- **NEIGHBOR Proposals**: Propose new peers for active or passive view.
- **Gossip for Membership**: Broadcast `JOIN`, `LEAVE`, and `FAIL` events via TTL-limited gossip.
- **Failure Detection**: Heartbeat-based liveness tracking and automatic removal of failed nodes.
- **Auto Eviction**: Maintain fixed-size active/passive views with LRU-based eviction.
- **Deduplicate Message**: An LRU-based cache system prevents duplicate messages from being processed.
- **Docker Compose & Bash Tests**: End-to-end protocol verification with containers and scripts.

---

## Testing

The script in the `/tests` directory drives a series of scenarios:

1. **bootstrap-test**: Verifies that the node that received the JOIN adds the member to its active view
2. **graceful-disconnect-test**: Verifies that a `gracefully` shutdown node does not appear in the membership of nodes in the cluster
3. **unexpected-disconnect-test**: Verifies that a unexpectedly shut down node does not appear in the membership of nodes in the cluster
4. **eldest-member-eviction-test**: Ensure that the eldest member is expelled to the passive view when the active view is full
5. **shuffle-view-fill-test**: Validate that active views are filled via `shuffle`
