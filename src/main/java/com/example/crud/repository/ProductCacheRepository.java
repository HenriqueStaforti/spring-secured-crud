package com.example.crud.repository;

import com.example.crud.dto.ProductResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class ProductCacheRepository {

    private static final Duration TTL = Duration.ofMinutes(10);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCacheRepository(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String keyForProduct(Long id) {
        return "product::" + id;
    }

    private String keyForProductPage(Pageable pageable) {
        return "products::" + pageable.getPageNumber() + ":" + pageable.getPageSize();
    }

    public void saveProduct(ProductResponseDTO dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(keyForProduct(dto.id()), json, TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar produto para cache", e);
        }
    }

    public Optional<ProductResponseDTO> findProduct(Long id) {
        String json = redisTemplate.opsForValue().get(keyForProduct(id));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ProductResponseDTO.class));
        } catch (Exception e) {
            redisTemplate.delete(keyForProduct(id));
            return Optional.empty();
        }
    }

    public void savePage(Pageable pageable, Page<ProductResponseDTO> page) {
        try {
            String json = objectMapper.writeValueAsString(page.getContent());
            redisTemplate.opsForValue().set(keyForProductPage(pageable), json, TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar lista para cache", e);
        }
    }

    public Optional<Page<ProductResponseDTO>> findPage(Pageable pageable) {
        String json = redisTemplate.opsForValue().get(keyForProductPage(pageable));
        if (json == null) return Optional.empty();
        try {
            List<ProductResponseDTO> list = objectMapper.readValue(
                    json,
                    new TypeReference<List<ProductResponseDTO>>() {}
            );
            long total = list.size();
            return Optional.of(new PageImpl<>(list, pageable, total));
        } catch (Exception e) {
            redisTemplate.delete(keyForProductPage(pageable));
            return Optional.empty();
        }
    }

    public void evictProduct(Long id) {
        redisTemplate.delete(keyForProduct(id));
    }

    public void clearProductPages() {
        Set<String> keys = redisTemplate.keys("products::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

}
