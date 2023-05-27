package com.ukeun.streamingservice.model.messages;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public record RtmpMediaMessage(RtmpHeader header, byte[] payload) {

    public static RtmpMediaMessage fromRtmpMessage(RtmpMessage message) {
        return new RtmpMediaMessage(message.header(), ByteBufUtil.getBytes(message.payload()));
    } // RtmpMessage 데이터를 RtmpMediaMessage 데이터로 변환.(payload : ByteBuf -> byte[])....외?

    public static RtmpMessage toRtmpMessage(RtmpMediaMessage message) {
        return new RtmpMessage(message.header(), Unpooled.wrappedBuffer(message.payload()));
    } // RtmpMediaMessage 데이터를 RtmpMessage 데이터로 변환.(payload : byte[] -> ByteBuf)

    public boolean isAudioConfig() {
        return this.payload.length > 1 && this.payload[1] == 0x00;
    } // ????????????

    //Is H.264 keyframe???????????????
    public boolean isKeyframe() {
        return this.payload.length > 1 && this.payload[0] == 0x17;
    }

    public boolean isVideoConfig() {
        return isKeyframe() && this.payload.length > 2 && this.payload[1] == 0x00;
    } // ??????????????
}
