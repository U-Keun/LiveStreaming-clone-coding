package com.ukeun.streamingservice.handlers;

import com.ukeun.streamingservice.model.messages.RtmpConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class HandshakeHandler extends ByteToMessageDecoder {

    private boolean C0C1;
    private boolean completed;
    private int timestamp;

    private byte[] clientBytes = new byte[RtmpConstants.RTMP_HANDSHAKE_SIZE - 8];

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws  Exception {
        if (completed) {
            // Continue pipeline
            channelHandlerContext.fireChannelRead(byteBuf);
            return;
        }
    }
}
