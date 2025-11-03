package com.example.crud.service;

import com.example.crud.model.ProductEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductPublisherService {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbit.product.exchange}")
    private String exchangeName;

    @Value("${app.rabbit.product.routing-key}")
    private String routingKey;

    public ProductPublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProductCreated(ProductEntity product) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, product);
        System.out.println("Sent product created message: " + product);
    }
}
