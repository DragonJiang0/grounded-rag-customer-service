package com.zidong.portal.config;

import com.zidong.portal.service.RpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author ZiDong
 * @create 2025 2025/7/2 18:22
 **/
@Configuration
public class RpcServerConfig {

    private Logger logger= LoggerFactory.getLogger(getClass());

    private final RpcService rpcService;

    /** gRPC 监听端口：测试里传 0 表示随机端口，避免与本机已启动的 portal 抢占 9091 */
    @Value("${portal.rpc.port:9091}")
    private int port;


    public RpcServerConfig(RpcService rpcService) {
        this.rpcService = rpcService;
    }


    @Bean
    public Server grpcServer(){
        logger.info("Starting gRPC server on port: {}", port);

        ServerBuilder serverBuilder = ServerBuilder.forPort(port);

        serverBuilder.addService(rpcService);

        return serverBuilder.build();
    }

    @Bean
    public CommandLineRunner grpcServerRunner(Server grpcServer) {
        return args -> {
            grpcServer.start();
            logger.info("gRPC Server started on port: {}", port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down RPC server...");
                grpcServer.shutdown();
                logger.info("RPC server shutdown initiated");
            }));

            new Thread(()->{
                try {
                    grpcServer.awaitTermination();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();

        };
    }
}

