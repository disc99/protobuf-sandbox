package io.disc99.grpc.spring.server;

import io.disc99.echo.v1.CreateEchoRequest;
import io.disc99.echo.v1.Echo;
import io.disc99.echo.v1.EchoServiceGrpc;
import io.disc99.echo.v1.GetEchoRequest;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringGRpcServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGRpcServerApplication.class, args);
    }

    @GRpcService
    static class EchoService extends EchoServiceGrpc.EchoServiceImplBase {
        @Override
        public void createEcho(CreateEchoRequest request, StreamObserver<Echo> responseObserver) {
            Echo echo = Echo.newBuilder()
                    .setMessage("OK")
                    .build();
            responseObserver.onNext(echo);
            responseObserver.onCompleted();
        }

        @Override
        public void getEcho(GetEchoRequest request, StreamObserver<Echo> responseObserver) {
            Echo echo = Echo.newBuilder()
                    .setMessage("Hello!")
                    .build();
            responseObserver.onNext(echo);
            responseObserver.onCompleted();
        }
    }
}
