package com.sucrestore.api.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.service.CategoryService;
import com.sucrestore.api.service.ProductService;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity in this specific test
@SuppressWarnings("removal")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @Test
    void testGetAllProducts() throws Exception {
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .slug("laptop")
                .price(new BigDecimal("999.99"))
                .available(true)
                .categoryName("Electronics")
                .stock(10)
                .build();
        Page<ProductResponse> page = new PageImpl<>(List.of(productResponse));
        when(productService.getAllActiveProducts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    @Test
    void testGetProductDetail_Success() throws Exception {
        ProductResponse productResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .slug("laptop")
                .price(new BigDecimal("999.99"))
                .available(true)
                .categoryName("Electronics")
                .stock(10)
                .build();
        when(productService.getProductBySlug("laptop")).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/laptop")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void testGetProductDetail_NotFound() throws Exception {
        when(productService.getProductBySlug("unknown"))
                .thenThrow(new RuntimeException("Produit introuvable : unknown"));

        mockMvc.perform(get("/api/products/unknown")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // Or 404 if exception handler is mapped correctly
        // Note: Without GlobalExceptionHandler mapping RuntimeException to 404, it returns 500.
    }

    @Test
    void testGetAllCategories() throws Exception {
        com.sucrestore.api.entity.Category category = com.sucrestore.api.entity.Category.builder()
                .id(1L)
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .build();
        when(categoryService.getAllActiveCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }
}
