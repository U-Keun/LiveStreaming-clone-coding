package com.ukeun.streamingservice.model.messages;

import lombok.Data;

@Data
public class RtmpHeader {
    int fmt; // format? 무엇의 format일까?
    int cid; // chunk stream Id 인 것 같다.
    /*
    chunk? 메시지의 조각. 메시지들은 작은 조각들로 나뉘어서 네트워크를 통해 보내지는데, 그 조각 하나를 chunk라고 부른다.
    chunk stream? chunk들이 특정한 흐름을 갖도록 하는 논리적 통신 채널(?). chunk stream은 클라이언트와 서버 사이를 오갈 수 있다.
     */
    int timestamp;
    int messageLength;
    short type;
    int streamId;
    int timestampDelta;
    long extendedTimestamp;
    int headerLength;
}
