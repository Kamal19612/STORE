package com.sucrestore.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sucrestore.api.dto.ProductRequest;
import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.repository.CategoryRepository;
import com.sucrestore.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Category category;
    private ProductRequest productRequest;

    @BeforeEach
    public void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .slug("laptop")
                .shortDescription("A powerful laptop")
                .description("Detailed description")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .active(true)
                .category(category)
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Laptop");
        productRequest.setSlug("laptop");
        productRequest.setCategoryId(1L);
        productRequest.setPrice(new BigDecimal("999.99"));
        productRequest.setStock(10);
        productRequest.setActive(true);
    }

    @Test
    void testGetAllActiveProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllActiveProducts(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
        verify(productRepository, times(1)).findByActiveTrue(pageable);
    }

    @Test
    void testGetProductBySlug_Success() {
        when(productRepository.findBySlug("laptop")).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductBySlug("laptop");

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(productRepository, times(1)).findBySlug("laptop");
    }

    @Test
    void testGetProductBySlug_NotFound() {
        when(productRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.getProductBySlug("unknown"));
        assertEquals("Produit introuvable : unknown", exception.getMessage());
        verify(productRepository, times(1)).findBySlug("unknown");
    }

    @Test
    void testCreateProduct() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse result = productService.createProduct(productRequest, "image-url.jpg");

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
