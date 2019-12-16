package io.disc99.grpc.spring.client;

import io.disc99.echo.v1.Echo;
import io.disc99.echo.v1.EchoServiceGrpc;
import io.disc99.echo.v1.GetEchoRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class SpringGRpcClientOnWebFluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGRpcClientOnWebFluxApplication.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> route() {
        EchoHandler echoHandler = new EchoHandler();
        return RouterFunctions.route(POST("/echo"), echoHandler::getEcho);
    }

    static class EchoHandler {
        EchoDataSource dataSource = new EchoDataSource();

        public Mono<ServerResponse> getEcho(ServerRequest serverRequest) {
            return serverRequest.bodyToMono(String.class)
                    .flatMap(req -> ok().body(dataSource.getEcho(req).map(Echo::toString), String.class));
        }
    }

    static class ReactiveStreamObserver<T> implements StreamObserver<T>, Publisher<T> {
        Subscriber<? super T> subscriber;

        @Override
        public void onNext(T value) {
            subscriber.onNext(value);
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void onCompleted() {
            subscriber.onComplete();
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            this.subscriber = s;
            this.subscriber.onSubscribe(new BaseSubscriber<T>() {
            });
        }
    }

    static class EchoDataSource {
        Mono<Echo> getEcho(String request) {
            ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:6565")
                    .usePlaintext()
                    .build();
            EchoServiceGrpc.EchoServiceStub stub = EchoServiceGrpc.newStub(channel);

            GetEchoRequest req = GetEchoRequest.newBuilder()
                    .build();

//            ReactiveStreamObserver<Echo> observer = new ReactiveStreamObserver<>();
//            stub.getEcho(req, observer);
//            return Mono.from(observer);
//
            return Mono.from(new Publisher<Echo>() {
                @Override
                public void subscribe(Subscriber<? super Echo> subscriber) {
                    subscriber.onSubscribe(new BaseSubscriber<>() {
                    });
                    stub.getEcho(req, new StreamObserver<>() {
                        @Override
                        public void onNext(Echo value) {
                            subscriber.onNext(value);
                        }

                        @Override
                        public void onError(Throwable t) {
                            subscriber.onError(t);
                        }

                        @Override
                        public void onCompleted() {
                            subscriber.onComplete();
                        }
                    });
                }
            });
        }
    }
}
