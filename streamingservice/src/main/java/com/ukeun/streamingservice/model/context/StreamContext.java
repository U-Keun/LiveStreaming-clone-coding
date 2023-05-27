package com.ukeun.streamingservice.model.context;

import java.util.concurrent.ConcurrentHashMap;

public class StreamContext {
    /*
    stream을 모아놓은 객체.
    key에는 streamName을, value에는 stream을 갖고 있다.
    아직 stream이 무엇인지는 잘 모르겠지만,
    어쨌든 StreamContext 객체는 stream을 관리하는 메서드들(추가, 삭제, 조회)을 갖고 있다.
     */
    public final ConcurrentHashMap<String, Stream> context;

    public StreamContext() {
        this.context = new ConcurrentHashMap<>();
    }

    public void addStream(Stream stream) {
        context.put(stream.getStreamName(), stream);
    }

    public void deleteStream(String streamName) {
        context.remove(streamName);
    }

    public Stream getStream(String streamName) {
        if (streamName == null) {
            return null;
        }
        return context.getOrDefault(streamName, null);
    }
}
