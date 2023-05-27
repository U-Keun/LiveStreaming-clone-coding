package com.ukeun.streamingservice.rtmp;

import com.ukeun.streamingservice.entity.User;
import com.ukeun.streamingservice.handlers.*;
import com.ukeun.streamingservice.model.context.Stream;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

import java.time.Duration;

@NoArgsConstructor
@Getter
@Setter
@Slf4j
@Configuration
public abstract class RtmpServer implements CommandLineRunner {
    /*
    CommandLineRunner interface :
    CommandLineRunner interface를 구현한 class의 run() 메서드를 Spring Boot App. 구동 시점에 실행시킨다.
    원래는 Bean으로 등록이 되어있어야 실행이 된다는데 qbasick 예제에는 없네. 없어도 되는 건가?
     */

    /*
    String 타입 currentSessionStream과
    StreamContext 타입 context를 가진 객체
     */
    protected abstract RtmpMessageHandler getRtmpMessageHandler();
    protected abstract InboundConnectionLogger getInboundConnectionLogger(); // 서버 콘솔에 로그 나오도록 설정
    protected abstract HandshakeHandler getHandshakeHandler();
    protected abstract ChunkDecoder getChunkDecoder();
    protected abstract ChunkEncoder getChunkEncoder();

    @Autowired
    private WebClient webClient;

    @Value("${transcoding.server}")
    private String transcodingAddress;

    @Value("${auth.server}")
    private String authAddress;

    @Override
    public void run(String... args) {
        DisposableServer server = TcpServer.create()
                // .host("0.0.0.0")
                .port(1935)
                // 아직 미지의 공간
                .doOnBound(disposableServer -> log.info("RTMP server started on port {}", disposableServer.port()))
                .doOnConnection(connection -> connection
                        .addHandlerLast(getInboundConnectionLogger())
                        .addHandlerLast(getHandshakeHandler())
                        .addHandlerLast(getChunkDecoder())
                        .addHandlerLast(getChunkEncoder())
                        .addHandlerLast(getRtmpMessageHandler()))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 뭔가 한다.
                .handle((in, out) -> in
                        .receiveObject() // 들어온 Flux 객체 데이터를
                        .cast(Stream.class) // Stream 클래스 객체로 바꿔서(Flux로 묶어서(?)) 보낸다.
                        // 변환된 Stream 객체를 가공하는 곳
                        .flatMap(stream ->
                                webClient // 클라이언트로서
                                        .post() // 아래의 URI 주소로 POST 요청을 보낸다.
                                        .uri(authAddress + "auth/check")
                                        // header에는 Content-Type : application/json으로
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        // body에는 streamName과 streamKey를 보낼 것이다.
                                        .body(Mono.just(new User(stream.getStreamName(), stream.getStreamKey())), User.class)
                                        // 위의 요청에 대한 응답을 어떻게 처리할 지 정한다.
                                        .retrieve()
                                        .bodyToMono(Boolean.class) // authservice로부터 Mono<Boolean>을 받을 것이다.
                                        // 응답이 늦으면 3번까지 시도할 거임.
                                        .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                        .doOnError(error -> log.info(error.getMessage())) // 에러 메시지 로그로 찍어볼 것이고,
                                        .onErrorReturn(Boolean.FALSE) // 오류나면 응답으로 False가 온 것으로 하겠다.
                                        .flatMap(ans -> { // ans로 Mono<Boolean> 값이 들어온 상태
                                            log.info("User {} stream key validation", stream.getStreamName()); // 일단 로그 찍기
                                            if (ans) { // 등록된 스트리머이다.
                                                stream.sendPublishMessage(); // 방송이 시작됨을 알린다. 내부 동작은 아직 모름.
                                                stream.getReadyToBroadcast().thenRun(() -> webClient // 다시 클라이언트로서
                                                        .get() // 아래의 URI 주소로 GET 요청을 보낸다.
                                                        .uri(transcodingAddress + "/ffmpeg/" + stream.getStreamName())
                                                        // 응답을 처리하도록 하자.
                                                        .retrieve()
                                                        .bodyToMono(Long.class) // transcodingservice로부터 Mono<Long>을 받아올 것이다.
                                                        // .delaySubscription(Duration.ofSeconds(10L)) // 방송 송출 지연을 말하는 것 같아요!
                                                        // 익숙한 재시도/예외 처리
                                                        .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1000)))
                                                        .doOnError(error -> { // 에러가 있으면 연결을 끊는다. 로그도 찍는다!
                                                            log.info("Error occured on transcoding server " + error.getMessage());
                                                            stream.closeStream();
                                                            stream.getPublisher().disconnect();
                                                        })
                                                        .onErrorComplete()
                                                        // subscribe -> 데이터를 보내기 시작함
                                                        .subscribe((s) -> log.info("Transcoding server started ffmpeg with pid " + s.toString())));
                                            } else { // 등록된 스트리머가 아니다.
                                                stream.getPublisher().disconnect();
                                            }
                                            return Mono.empty();
                                        }))
                        .then())
                .bindNow(); // ?
        server.onDispose().block(); // ?
    }
}
