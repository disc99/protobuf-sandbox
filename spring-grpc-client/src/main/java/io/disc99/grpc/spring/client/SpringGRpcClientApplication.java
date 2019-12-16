package io.disc99.grpc.spring.client;

import io.disc99.echo.v1.Echo;
import io.disc99.echo.v1.EchoServiceGrpc;
import io.disc99.echo.v1.GetEchoRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SpringGRpcClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringGRpcClientApplication.class, args);
    }

    @RestController
    static class EchoController {
        @GetMapping("/echo")
        String get() {
            ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:6565")
                    .usePlaintext()
                    .build();
            EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channel);

            GetEchoRequest request = GetEchoRequest.newBuilder().build();
            Echo response = stub.getEcho(request);
            return response.getMessage();
        }
    }

}
