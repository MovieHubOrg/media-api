package com.media.api.service.rabbit;

import com.media.api.component.ServerConfigHolder;
import com.media.api.dto.ServerConfigDto;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class RabbitConfiguration {
    @Value("${rabbitmq.streaming.server-queue}")
    private String streamingQueue;

    @Value("${server.number}")
    private String serverNumber;

    @Value("${rabbitmq.convert.video.queue}")
    private String convertVideoQueue;

    @Value("${rabbitmq.update.video.queue}")
    private String updateVideoQueue;

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue streamingQueue() {
        return new Queue(streamingQueue, true);
    }

    @Bean
    public Queue convertVideoQueue() {
        return new Queue(convertVideoQueue, true);
    }

    @Bean
    public Queue updateVideoQueue() {
        return new Queue(updateVideoQueue, true);
    }

    @Bean(name = "convertExecutor")
    public Executor convertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // Số tác vụ convert chạy song song
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5); // Số job chờ
        executor.setThreadNamePrefix("ConvertThread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("convertQueueFactory")
    public SimpleRabbitListenerContainerFactory convertQueueFactory(
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Dùng thủ công để chủ động ack
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1); // chỉ lấy 1 message tại 1 thời điểm
        return factory;
    }
}
