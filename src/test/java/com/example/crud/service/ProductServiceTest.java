package com.example.crud.service;

import com.example.crud.dto.ProductResponseDTO;
import com.example.crud.dto.ProductUpdateRequestDTO;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.mapper.ProductMapper;
import com.example.crud.model.ProductEntity;
import com.example.crud.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private ProductEntity sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = ProductEntity.builder()
                .id(1L)
                .name("Product 1")
                .price(100.0)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void create_shouldPersistAndReturnProduct() {
        when(productRepository.save(any(ProductEntity.class))).thenReturn(sampleProduct);

        ProductEntity toSave = ProductEntity.builder().name("New").price(10.0).enabled(true).build();
        ProductResponseDTO result = productService.create(toSave);

        assertNotNull(result);
        assertEquals(sampleProduct.getId(), result.id());
        verify(productRepository).save(toSave);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void get_shouldReturnProduct_whenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponseDTO result = productService.get(1L);

        assertEquals(1L, result.id());
        verify(productRepository).findById(1L);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void get_shouldThrowResourceNotFound_whenMissing() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.get(999L));
        verify(productRepository).findById(999L);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void list_shouldReturnPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductEntity> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductResponseDTO> result = productService.list(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(sampleProduct.getId(), result.getContent().get(0).id());
        verify(productRepository).findAll(pageable);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void update_shouldMapAndSave_whenFound() {
        ProductUpdateRequestDTO dto = new ProductUpdateRequestDTO("Updated", 200.0, false);
        ProductEntity existing = ProductEntity.builder()
                .id(1L)
                .name("Old")
                .price(50.0)
                .enabled(true)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        // mapper mutates existing entity; we just verify interaction
        doAnswer(invocation -> {
            ProductUpdateRequestDTO d = invocation.getArgument(0);
            ProductEntity e = invocation.getArgument(1);
            if (d.name() != null) e.setName(d.name());
            if (d.price() != null) e.setPrice(d.price());
            if (d.enabled() != null) e.setEnabled(d.enabled());
            return null;
        }).when(productMapper).updateEntityFromUpdateDto(eq(dto), eq(existing));

        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponseDTO result = productService.update(1L, dto);

        assertEquals("Updated", result.name());
        assertEquals(200.0, result.price());
        assertFalse(result.enabled());

        verify(productRepository).findById(1L);
        verify(productMapper).updateEntityFromUpdateDto(dto, existing);
        verify(productRepository).save(existing);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void update_shouldThrowResourceNotFound_whenMissing() {
        ProductUpdateRequestDTO dto = new ProductUpdateRequestDTO("Updated", 200.0, false);
        when(productRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(123L, dto));
        verify(productRepository).findById(123L);
        verifyNoInteractions(productMapper);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void delete_shouldDelegateToRepository() {
        doNothing().when(productRepository).deleteById(1L);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
        verifyNoMoreInteractions(productRepository, productMapper);
    }
}
