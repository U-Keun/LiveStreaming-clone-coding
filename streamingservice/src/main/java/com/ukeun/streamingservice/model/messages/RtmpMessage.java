package com.ukeun.streamingservice.model.messages;

import io.netty.buffer.ByteBuf;

public record RtmpMessage(RtmpHeader header, ByteBuf payload) {
    /*
    record? 순수하게 데이터를 보유하기 위한 특수한 종류의 클래스
    RtmpMessage record는
    RtmpHeader 타입 변수 header와
    ByteBuf 변수 payload를 가진다는 의미이다.
    데이터를 메서드처럼 받아서 쓰는 녀석인 것 같다.
     */
}
