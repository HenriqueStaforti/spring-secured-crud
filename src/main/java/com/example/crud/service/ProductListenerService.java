package com.example.crud.service;

import com.example.crud.model.ProductEntity;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductListenerService {

    @RabbitListener(queues = "${app.rabbit.product.queue}")
    public void listenProductCreated(ProductEntity product) {
        System.out.println("Received product created message: " + product);
    }
}
