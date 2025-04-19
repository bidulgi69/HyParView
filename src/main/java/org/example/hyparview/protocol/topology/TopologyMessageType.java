package org.example.hyparview.protocol.topology;

public enum TopologyMessageType {

    JOIN,
    FWD_JOIN,
    FWD_JOIN_REPLY,
    SHUFFLE_REQUEST,
    SHUFFLE_REPLY,
    NEIGHBOR,
    DISCONNECT,
}

/*
메시지 타입 | 방향 | 주요 필드 | 역할
JOIN | Z → A | nodeId, address | 새 노드가 Bootstrap 노드에 합류 요청
FWD_JOIN | A → B | sourceId, nodeId, address, ttl | JOIN 요청을 TTL만큼 중계
FWD_JOIN_REPLY | B → Z | acceptorId, address | 최종 수신 노드가 Z에게 “Active view에 추가할게요” 응답
SHUFFLE_REQUEST | A → B | originId, sampleSet, ttl | Active/Passive view 섞기 위해 샘플 셋을 TTL만큼 중계
SHUFFLE_REPLY | B → A | originId, replySet | TTL 소진 시점의 노드가 샘플 셋을 되돌려 보내는 응답
NEIGHBOR | A → Z or B | nodeId, address, activeFlag | “내 뷰에 추가해도 될까요?” (activeFlag로 Active/Passive 구분)
DISCONNECT | A → B | nodeId | “너랑 끊을게”—Active view에서 제거 요청
 */