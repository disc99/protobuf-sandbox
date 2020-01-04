package io.disc99.grpc.spring.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
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
import org.springframework.http.MediaType;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class SpringGRpcClientOnWebFluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGRpcClientOnWebFluxApplication.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> route() {
        EchoHandler echoHandler = new EchoHandler();
        return RouterFunctions.route()
                .POST("/echo", echoHandler::getEcho)
                .POST("/echo2", echoHandler::geEcho2)
                .build();
    }

    @Component
    static class ProtobufJsonDecoder extends ProtobufDecoder {
        static final List<MimeType> MIME_TYPES = Collections.singletonList(new MimeType("application", "json"));

        @Override
        protected List<MimeType> getMimeTypes() {
            return MIME_TYPES;
        }
    }

    static class EchoHandler {
        EchoDataSource dataSource = new EchoDataSource();
        JsonFormat.Printer printer = JsonFormat.printer();
        JsonFormat.Parser parser = JsonFormat.parser();

        @Nonnull
        public Mono<ServerResponse> getEcho(ServerRequest serverRequest) {
            return serverRequest.bodyToMono(String.class)
                    .flatMap(req -> ok().body(dataSource.getEcho(req).map(Echo::toString), String.class));
        }

        // gRPC request calling and response json mapping
        @Nonnull
        public Mono<ServerResponse> geEcho2(ServerRequest serverRequest) {
            return Mono.just(Echo.newBuilder())

                    // Parse Request
                    .flatMap(builder -> serverRequest.bodyToMono(String.class)
                            .map(json -> {
                                try {
                                    parser.merge(json, builder);
                                    return builder;
                                } catch (InvalidProtocolBufferException e) {
                                    throw Exceptions.propagate(e);
                                }
                            }))
                    .map(builder -> {
                        boolean hasPathVariable = false; // TODO
                        boolean hasParameter = false; // TODO

                        if (hasPathVariable) {

                        }

                        if (hasParameter) {


                        }

                        Echo message = builder.build();
                        return message;
                    })

                    // Call gRPC
                    // TODO

                    // Create Response
                    .flatMap(res -> {
                        try {
                            String json = printer.print(res);
                            return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(json);
                        } catch (InvalidProtocolBufferException e) {
                            return Mono.error(e);
                        }
                    });
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

            ReactiveStreamObserver<Echo> observer = new ReactiveStreamObserver<>();
            stub.getEcho(req, observer);
            return Mono.from(observer);
        }
    }
}
