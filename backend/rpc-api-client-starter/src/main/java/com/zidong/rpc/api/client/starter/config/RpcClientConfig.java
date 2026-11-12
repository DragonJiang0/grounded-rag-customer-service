package com.zidong.rpc.api.client.starter.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author ZiDong
 * @create 2025 2025/7/2 18:43
 **/
@EnableConfigurationProperties({RpcClientProp.class})
@Configuration
public class RpcClientConfig {

    private Logger logger= LoggerFactory.getLogger(getClass());

    private final RpcClientProp rpcClientProp;

    private  Map<String, ManagedChannel>channelMap=new HashMap<>();

    /** 配置缺失或非法时的兜底超时（秒），避免 deadline 为 0 导致调用瞬间失败 */
    private static final long FALLBACK_DEADLINE_SECONDS = 300L;

    public RpcClientConfig(RpcClientProp rpcClientProp) {
        this.rpcClientProp = rpcClientProp;
    }

    public ManagedChannel getChannel(String channel){
        ManagedChannel managedChannel = channelMap.get(channel);
        if(managedChannel==null){
            logger.error("未找到RPC服务：{}",channel);
        }
        return managedChannel;
    }

    /**
     * 取某个 RPC 服务生效的调用超时（秒）：服务单独配了用单独的，否则用全局默认。
     *
     * @param channel 服务名，与 {@link RpcClientProp.RpcConfig#getName()}、{@code RpcServerEnum} 对齐
     */
    public long getDeadlineSeconds(String channel) {
        long global = rpcClientProp.getDeadlineSeconds() > 0
                ? rpcClientProp.getDeadlineSeconds()
                : FALLBACK_DEADLINE_SECONDS;
        if (channel == null || rpcClientProp.getConfigs() == null) {
            return global;
        }
        return rpcClientProp.getConfigs().stream()
                .filter(config -> channel.equals(config.getName()))
                .findFirst()
                .map(RpcClientProp.RpcConfig::getDeadlineSeconds)
                .filter(seconds -> seconds != null && seconds > 0)
                .orElse(global);
    }


    @PostConstruct
    public void init(){
    this.rpcClientProp.getConfigs().stream().forEach(a->{
        logger.info("初始化RPC客户端：{}",a.getName());
        ManagedChannel channel = ManagedChannelBuilder.forAddress(a.getHost(), a.getPort()).usePlaintext().build();
        channelMap.put(a.getName(),channel);
    });
    }

  @PreDestroy
  public void destroy() {
        channelMap.forEach((serviceName, channel) -> shutdownChannel(channel, serviceName));
    }

    private void shutdownChannel(ManagedChannel channel, String serviceName) {
        if (channel != null && !channel.isShutdown()) {
            System.out.println("Shutting down " + serviceName + " RPC channel");
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println(serviceName + " RPC channel did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
