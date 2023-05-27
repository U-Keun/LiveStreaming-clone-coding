package com.ukeun.streamingservice.handlers;

import com.ukeun.streamingservice.model.context.StreamContext;
import com.ukeun.streamingservice.model.messages.RtmpMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RtmpMessageHandler extends MessageToMessageDecoder<RtmpMessage> {
    /*
    RtmpMessage를 가공한다.
    다시 말하면, Rtmpheader 타입의 header와
    ByteBuf 타입의 payload를 가공할 것이다.
     */

    private String currentSessionStream;
    private final StreamContext context; // stream을 Map으로 갖고 있는 객체

    public RtmpMessageHandler(StreamContext context) {
        this.context = context;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, RtmpMessage in, List<Object> out) {

    }
}
