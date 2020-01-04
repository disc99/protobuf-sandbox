package io.disc99.grpc.spring.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
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
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
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
                .POST("/echo_with_text", echoHandler::getEcho)
                .POST("/echo_with_json", echoHandler::geEcho2)
                .POST("/echo_with_protobuf_json_codec", echoHandler::geEcho3)
                .build();
    }

    @Bean
    CodecCustomizer protobufJsonCodec() {
        return configurer -> {
            CodecConfigurer.CustomCodecs customCodecs = configurer.customCodecs();
            customCodecs.decoder(new ProtobufJsonDecoder());
            customCodecs.encoder(new ProtobufJsonEncoder());
        };
    }

    static class ProtobufJsonDecoder extends ProtobufDecoder {
        private static final List<MimeType> MIME_TYPES = Collections.singletonList(new MimeType("application", "json"));

        @Override
        public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
            return Message.class.isAssignableFrom(elementType.toClass()) && MIME_TYPES.contains(mimeType);
        }

        @Override
        public Message decode(DataBuffer dataBuffer, ResolvableType targetType, MimeType mimeType, Map<String, Object> hints) throws DecodingException {
            try {
                Message.Builder builder = getMessageBuilder(targetType.toClass());
                JsonFormat.parser().merge(new InputStreamReader(dataBuffer.asInputStream()), builder);
                return builder.build();
            } catch (IOException ex) {
                throw new DecodingException("I/O error while parsing input stream", ex);
            } catch (Exception ex) {
                throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
        }

        private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
            Method method = clazz.getMethod("newBuilder");
            return (Message.Builder) method.invoke(clazz);
        }
    }

    static class ProtobufJsonEncoder extends ProtobufEncoder {
        private static final List<MimeType> MIME_TYPES = Collections.singletonList(new MimeType("application", "json"));

        @Override
        public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
            return Message.class.isAssignableFrom(elementType.toClass()) && MIME_TYPES.contains(mimeType);
        }

        @Override
        public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, ResolvableType valueType, MimeType mimeType, Map<String, Object> hints) {
            DataBuffer buffer = bufferFactory.allocateBuffer();
            boolean release = true;
            try {
                String json = JsonFormat.printer().print(message);
                buffer.write(json, UTF_8);
                release = false;
                return buffer;
            } catch (IOException ex) {
                throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
            } finally {
                if (release) {
                    DataBufferUtils.release(buffer);
                }
            }
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

        // gRPC request calling and response json mapping
        @Nonnull
        public Mono<ServerResponse> geEcho3(ServerRequest serverRequest) {
            return Mono.just(Echo.newBuilder())

                    // Parse Request
                    .flatMap(builder -> serverRequest.bodyToMono(Echo.class).map(Echo::toBuilder))
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
                    .flatMap(res -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(res));
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
