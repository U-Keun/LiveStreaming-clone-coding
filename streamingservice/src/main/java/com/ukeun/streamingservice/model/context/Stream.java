package com.ukeun.streamingservice.model.context;

import com.ukeun.streamingservice.model.messages.RtmpMediaMessage;
import com.ukeun.streamingservice.model.messages.RtmpMessage;
import com.ukeun.streamingservice.model.util.MessageProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static com.ukeun.streamingservice.model.messages.RtmpConstants.*;

@Getter
@Setter
@Slf4j
public class Stream {

    private Map<String, Object> metadata;
    /*
    Netty의 Channel interface는 소켓이나 I/O의 처리가 가능한 읽기, 쓰기, 연결하기, 바인딩과 같은 기능을 제공한다. <- 뭔 말?
    지금은 일단 스트리머 또는 시청자와 대응되는 객체라고 생각하자. 아닝가?
     */
    private Channel publisher;

    private final Set<Channel> subscribers;
    private final String streamName;
    private String streamKey;

    // Group of pictures chache to broadcast. Cleared if keyframe is received <- ??
    private final BlockingQueue<RtmpMediaMessage> rtmpGopCache;

    private RtmpMediaMessage videoConfig;
    private RtmpMediaMessage audioConfig;

    private CompletableFuture<Boolean> readyToBroadcast;

    public Stream(String streamName) {
        this.streamName = streamName;
        this.subscribers = new LinkedHashSet<>();
        this.rtmpGopCache = new ArrayBlockingQueue<>(1024);
        this.readyToBroadcast = new CompletableFuture<>();
    }

    public void addMedia(RtmpMediaMessage message) {
        short type = message.header().getType();

        if (type == (short) RTMP_MSG_USER_CONTROL_TYPE_AUDIO) {
            if (message.isAudioConfig()) {
                log.info("Audio config is set");
                audioConfig = message;
            }
        } else if (type == (short) RTMP_MSG_USER_CONTROL_TYPE_VIDEO) {
            if (message.isVideoConfig()) {
                log.info("Video config is set");
                videoConfig = message;
            }
            // clear interFrames queue
            if (message.isKeyframe()) {
                log.info("Keyframe added. {} frames cleared", rtmpGopCache.size());
                rtmpGopCache.clear();
            }
        }
        rtmpGopCache.add(message);
        broadcastMessage(message);
    }

    public void broadcastMessage(RtmpMediaMessage message) {
        if (!readyToBroadcast.isDone()) {
            readyToBroadcast.complete(Boolean.TRUE);
        }
        Iterator<Channel> channelIterator = subscribers.iterator();
        while (channelIterator.hasNext()) {
            Channel next = channelIterator.next();
            if (next.isActive()) {
                next.writeAndFlush(RtmpMediaMessage.toRtmpMessage(message));
            } else {
                log.info("Inactive channel detected");
                channelIterator.remove();
            }
        }
    }

    public void addSubscriber(Channel channel) {
        log.info("Subscriber {} added to stream {}", channel.remoteAddress(), streamName);
        subscribers.add(channel);

        channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(videoConfig));
        channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(audioConfig));

        log.info("Sending group of pictures to client");
        for (RtmpMediaMessage message:rtmpGopCache) {
            channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(message));
        }
    }

    public void closeStream() {

        log.info("Closing stream");
        RtmpMessage eof = MessageProvider.userControlMessageEvent(STREAM_EOF);
        for (Channel channel : subscribers) {
            channel.writeAndFlush(eof).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void sendPublishMessage() {
        publisher.writeAndFlush(MessageProvider.onStatus(
                "status",
                "NetStream.Publish.Start",
                "Start publishing"));
    }

}