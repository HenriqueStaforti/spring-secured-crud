package com.example.crud.service;

import com.example.crud.dto.ProductResponseDTO;
import com.example.crud.dto.ProductUpdateRequestDTO;
import com.example.crud.exception.ResourceNotFoundException;
import com.example.crud.mapper.ProductMapper;
import com.example.crud.model.ProductEntity;
import com.example.crud.repository.ProductCacheRepository;
import com.example.crud.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductPublisherService productPublisherService;
    private final ProductCacheRepository cache;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, ProductPublisherService productPublisherService, ProductCacheRepository cache) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.productPublisherService = productPublisherService;
        this.cache = cache;
    }

    @Transactional
    public ProductResponseDTO create(ProductEntity product) {
        ProductEntity entity = productRepository.save(product);

        System.out.println("Product created: " + entity);
        productPublisherService.publishProductCreated(entity);

        return productMapper.toResponseDto(entity);
    }

    public ProductResponseDTO get(Long id) {
        return cache.findProduct(id).orElseGet(() -> {
            ProductEntity entity = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            ProductResponseDTO dto = productMapper.toResponseDto(entity);
            cache.saveProduct(dto);
            return dto;
        });
    }

    public Page<ProductResponseDTO> list(Pageable pageable) {
        return cache.findPage(pageable).orElseGet(() -> {
            Page<ProductResponseDTO> page = productRepository.findAll(pageable).map(productMapper::toResponseDto);
            cache.savePage(pageable, page);
            return page;
        });
    }

    @Transactional
    public ProductResponseDTO update(Long id, ProductUpdateRequestDTO updateDto) {
        ProductEntity existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productMapper.updateEntityFromUpdateDto(updateDto, existing);
        ProductEntity saved = productRepository.save(existing);
        ProductResponseDTO dto = productMapper.toResponseDto(saved);

        cache.saveProduct(dto);
        cache.clearProductPages();

        return dto;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);

        cache.evictProduct(id);
        cache.clearProductPages();
    }
}
