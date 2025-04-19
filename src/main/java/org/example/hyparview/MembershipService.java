package org.example.hyparview;

import org.example.hyparview.configuration.HyparViewProperties;
import org.example.hyparview.event.MemberViewChangeEvent;
import org.example.hyparview.event.MembershipEventDispatcher;
import org.example.hyparview.protocol.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MembershipService {

    private final Node me;
    private final int activeViewSizeLimit;
    private final int passiveViewSizeLimit;
    private final MembershipEventDispatcher dispatcher;

    // size: log(n)+1
    private final Map<String, Member> activeView;
    private final PriorityQueue<Member> activeMemberLastSeenMinHeap;
    // size: activeView *6
    private final Map<String, Member> passiveView;
    private final PriorityQueue<Member> passiveMemberLastSeenMinHeap;
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public MembershipService(HyparViewProperties properties,
                             MembershipEventDispatcher dispatcher
    ) {
        me = properties.getId();
        activeViewSizeLimit = 1 + (int)Math.log(properties.getNetworkScale());
        passiveViewSizeLimit = 6 * activeViewSizeLimit;
        this.dispatcher = dispatcher;

        activeView = new HashMap<>(activeViewSizeLimit);
        activeMemberLastSeenMinHeap = new PriorityQueue<>(activeViewSizeLimit, Comparator.comparing(Member::getLastSeen));
        passiveView = new HashMap<>(passiveViewSizeLimit);
        passiveMemberLastSeenMinHeap =  new PriorityQueue<>(passiveViewSizeLimit, Comparator.comparing(Member::getLastSeen));
    }

    public void join(Member member) {
        lock.lock();
        try {
            if (me.nodeId().equals(member.getId()) || activeView.containsKey(member.getId())) {
                return;
            }

            if (!enoughSpaceInActiveView() && !passiveView.containsKey(member.getId())) {
                if (!enoughSpaceInPassiveView()) {
                    Member eldestPassiveMember = getEldestPassiveMember();
                    passiveView.remove(eldestPassiveMember.getId());
                    passiveMemberLastSeenMinHeap.remove(eldestPassiveMember);
                }
                passiveView.put(member.getId(), member);
                passiveMemberLastSeenMinHeap.add(member);
            } else {
                activeView.put(member.getId(), member);
                activeMemberLastSeenMinHeap.add(member);
            }
        } finally {
            lock.unlock();
        }
    }

    public void forceJoin(Member member) {
        lock.lock();
        try {
            if (me.nodeId().equals(member.getId()) || activeView.containsKey(member.getId())) {
                return;
            }

            if (!enoughSpaceInActiveView()) {
                // 가장 오래된 active member 를 passive view 로 이동
                Member eldestActiveMember = getEldestActiveMember();
                activeView.remove(eldestActiveMember.getId());
                activeMemberLastSeenMinHeap.remove(eldestActiveMember);
                passiveView.put(eldestActiveMember.getId(), eldestActiveMember);
                passiveMemberLastSeenMinHeap.add(eldestActiveMember);
            }
            activeView.put(member.getId(), member);
            activeMemberLastSeenMinHeap.add(member);
        } finally {
            lock.unlock();
        }
    }

    public void joinIfAvailable(Member member, boolean active) {
        lock.lock();
        try {
            if (me.nodeId().equals(member.getId())) {
                return;
            }

            if (active && enoughSpaceInActiveView() && !activeView.containsKey(member.getId())) {
                activeView.put(member.getId(), member);
                activeMemberLastSeenMinHeap.add(member);
            } else if (!active && enoughSpaceInPassiveView() && !passiveView.containsKey(member.getId())) {
                passiveView.put(member.getId(), member);
                passiveMemberLastSeenMinHeap.add(member);
            }
        } finally {
            lock.unlock();
        }
    }

    public void disconnect(String nodeId) {
        if (activeView.containsKey(nodeId)) {
            Member activeMember = activeView.remove(nodeId);
            activeMemberLastSeenMinHeap.remove(activeMember);
            // move passive member to active view
            Member passiveMember = getRandomPassiveMember();
            if (passiveMember != null) {
                activeView.put(passiveMember.getId(), passiveMember);
                activeMemberLastSeenMinHeap.add(passiveMember);
                passiveView.remove(passiveMember.getId());
                passiveMemberLastSeenMinHeap.remove(passiveMember);
            }
        }
        Member disconnected = passiveView.remove(nodeId);
        if (disconnected != null) {
            passiveMemberLastSeenMinHeap.remove(disconnected);
        }
    }

    public void mergeIntoActiveView(Collection<Member> members) {
        for (Member member : members) {
            if (me.nodeId().equals(member.getId()) || activeView.containsKey(member.getId())) {
                continue;
            }

            lock.lock();
            try {
                if (enoughSpaceInActiveView()) {
                    // passive view 에 이미 존재했다면 active view 로 이동시킨다.
                    if (passiveView.containsKey(member.getId())) {
                        passiveView.remove(member.getId());
                        passiveMemberLastSeenMinHeap.remove(member);
                    }
                    activeView.put(member.getId(), member);
                    activeMemberLastSeenMinHeap.add(member);
                    dispatcher.dispatch(new MemberViewChangeEvent(member, true));
                } else if (!passiveView.containsKey(member.getId())) {
                    if (!enoughSpaceInPassiveView()) {
                        // 가장 오래전에 갱신된 passive member 를 추방
                        Member eldestPassiveMember = getEldestPassiveMember();
                        if (eldestPassiveMember != null) {
                            passiveView.remove(eldestPassiveMember.getId());
                            passiveMemberLastSeenMinHeap.remove(eldestPassiveMember);
                        }
                    }
                    passiveView.put(member.getId(), member);
                    passiveMemberLastSeenMinHeap.add(member);
                    dispatcher.dispatch(new MemberViewChangeEvent(member, false));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void mergeIntoPassiveView(Collection<Member> members) {
        for (Member member : members) {
            // 이미 알고있는 멤버인 경우 무시
            if (me.nodeId().equals(member.getId()) ||
                activeView.containsKey(member.getId()) ||
                passiveView.containsKey(member.getId())
            ) {
                continue;
            }

            lock.lock();
            try {
                if (enoughSpaceInPassiveView()) {
                    passiveView.put(member.getId(), member);
                    passiveMemberLastSeenMinHeap.add(member);
                    dispatcher.dispatch(new MemberViewChangeEvent(member, false));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void applyHeartbeat(String memberId, Instant heartbeatAt) {
        Member member;
        if (activeView.containsKey(memberId)) {
            member = activeView.get(memberId);
            activeMemberLastSeenMinHeap.remove(member);
            member.updateLastSeen(heartbeatAt);
            activeMemberLastSeenMinHeap.add(member);
        } else if (passiveView.containsKey(memberId)) {
            member = passiveView.get(memberId);
            passiveMemberLastSeenMinHeap.remove(member);
            member.updateLastSeen(heartbeatAt);
            passiveMemberLastSeenMinHeap.add(member);
        }
    }

    private boolean enoughSpaceInActiveView() {
        return activeViewSizeLimit > activeView.size();
    }

    private boolean enoughSpaceInPassiveView() {
        return passiveViewSizeLimit > passiveView.size();
    }

    public int getFanoutSize() {
        return Math.max(1, (int)Math.log(activeView.size()));
    }

    public List<Member> getRandomActiveMembersLimit(int limit) {
        List<Member> activeMembers = new ArrayList<>(activeView.values());
        Collections.shuffle(activeMembers);
        return activeMembers.subList(0, Math.min(limit, activeMembers.size()));
    }

    public List<Member> getRandomPassiveMembersLimit(int limit) {
        List<Member> passiveMembers = new ArrayList<>(passiveView.values());
        Collections.shuffle(passiveMembers);
        return passiveMembers.subList(0, Math.min(limit, passiveMembers.size()));
    }

    public Member getRandomActiveMember() {
        if (activeView.isEmpty()) {
            return null;
        }

        List<Member> activeMembers = new ArrayList<>(activeView.values());
        Collections.shuffle(activeMembers);

        return activeMembers.get(0);
    }

    public Member getRandomPassiveMember() {
        if (passiveView.isEmpty()) {
            return null;
        }

        List<Member> passiveMembers = new ArrayList<>(passiveView.values());
        Collections.shuffle(passiveMembers);
        return passiveMembers.get(0);
    }

    private Member getEldestActiveMember() {
        return activeMemberLastSeenMinHeap.peek();
    }

    private Member getEldestPassiveMember() {
        return passiveMemberLastSeenMinHeap.peek();
    }

    // for testing
    public Collection<Member> getActiveMembers() {
        return activeView.values();
    }

    public Collection<Member> getPassiveMembers() {
        return passiveView.values();
    }

    public int getActiveViewSizeLimit() {
        return activeViewSizeLimit;
    }

    public int getPassiveViewSizeLimit() {
        return passiveViewSizeLimit;
    }
}
