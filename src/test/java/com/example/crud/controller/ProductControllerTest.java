package com.example.crud.controller;

import com.example.crud.dto.ProductRequestDTO;
import com.example.crud.dto.ProductResponseDTO;
import com.example.crud.mapper.ProductMapper;
import com.example.crud.model.ProductEntity;
import com.example.crud.service.JwtService;
import com.example.crud.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProductMapper productMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldReturnProductResponseWhenCreateProductSuccessfully() throws Exception {
        ProductRequestDTO productRequestDTO = buildProductRequest("Product 1", 123.99, true);
        ProductResponseDTO expectedResponse = buildProductResponse(1L, "Product 1", 123.99, true, Instant.now());

        when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(new ProductEntity());
        when(productMapper.toResponseDto(any(ProductEntity.class))).thenReturn(expectedResponse);
        when(productService.create(any(ProductEntity.class))).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productRequestDTO)))
            .andExpect(status().isOk())
            .andReturn();

        ProductResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductResponseDTO.class
        );

        assertEquals(expectedResponse.name(), response.name());
        assertEquals(expectedResponse.price(), response.price());
        assertEquals(expectedResponse.enabled(), response.enabled());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldReturn400WhenCreateProductWithInvalidName() throws Exception {
        ProductRequestDTO productRequestDTO = buildProductRequest("", 123.99, true);

        mockMvc.perform(post("/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(productRequestDTO)))
            .andExpect(status().isBadRequest());
    }

    private ProductRequestDTO buildProductRequest(String name, double price, boolean enabled) {
        return new ProductRequestDTO(name, price, enabled);
    }

    private ProductResponseDTO buildProductResponse(Long id, String name, double price, boolean enabled, Instant createdAt) {
        return new ProductResponseDTO(id, name, price, enabled, createdAt);
    }
}
