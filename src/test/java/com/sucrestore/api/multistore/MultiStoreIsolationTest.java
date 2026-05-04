package com.sucrestore.api.multistore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucrestore.api.dto.OrderRequest;
import com.sucrestore.api.dto.OrderItemRequest;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.entity.Store;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.CategoryRepository;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.ProductRepository;
import com.sucrestore.api.repository.StoreRepository;
import com.sucrestore.api.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiStoreIsolationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired StoreRepository storeRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;

    Store sucre;
    Store spirit;
    Product sucreProduct;
    Product spiritProduct;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        sucre = storeRepository.save(Store.builder().code("sucre").name("SUCRE STORE").build());
        spirit = storeRepository.save(Store.builder().code("spirit").name("SPIRIT STORE").build());

        Category catSucre = categoryRepository.save(Category.builder().store(sucre).name("CatSucre").slug("catsucre").active(true).build());
        Category catSpirit = categoryRepository.save(Category.builder().store(spirit).name("CatSpirit").slug("catspirit").active(true).build());

        sucreProduct = productRepository.save(Product.builder()
            .store(sucre)
            .name("Sucre Product")
            .slug("sucre-product")
            .price(new BigDecimal("1000"))
            .stock(10)
            .active(true)
            .mainImage("x")
            .category(catSucre)
            .build());

        spiritProduct = productRepository.save(Product.builder()
            .store(spirit)
            .name("Spirit Product")
            .slug("spirit-product")
            .price(new BigDecimal("2000"))
            .stock(10)
            .active(true)
            .mainImage("x")
            .category(catSpirit)
            .build());

        // Store-scoped admin user
        userRepository.save(User.builder()
            .store(sucre)
            .username("admin_sucre")
            .email("admin_sucre@test.local")
            .password("x")
            .role(User.Role.ADMIN)
            .active(true)
            .build());

        // Global delivery user (no store)
        userRepository.save(User.builder()
            .store(null)
            .username("delivery")
            .email("delivery@test.local")
            .password("x")
            .role(User.Role.DELIVERY)
            .active(true)
            .build());
    }

    @Test
    void publicCatalog_isolatedByHeaderStoreCode() throws Exception {
        mockMvc.perform(get("/api/products")
            .header("X-Store-Code", "sucre"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("sucre-product"));

        mockMvc.perform(get("/api/products")
            .header("X-Store-Code", "spirit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("spirit-product"));
    }

    @Test
    void orders_createdInRightStore_andAdminCannotCrossStore() throws Exception {
        OrderRequest reqSucre = new OrderRequest();
        reqSucre.setCustomerName("A");
        reqSucre.setCustomerPhone("+226 000");
        reqSucre.setCustomerAddress("Addr");
        reqSucre.setDeliveryType("STANDARD");
        OrderItemRequest it1 = new OrderItemRequest();
        it1.setProductId(sucreProduct.getId());
        it1.setQuantity(1);
        reqSucre.setItems(List.of(it1));
        reqSucre.setDeliveryCost(new BigDecimal("0"));
        reqSucre.setDistance(new BigDecimal("1"));
        reqSucre.setTotalAmount(new BigDecimal("1000"));

        mockMvc.perform(post("/api/orders")
            .header("X-Store-Code", "sucre")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reqSucre)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").exists());

        OrderRequest reqSpirit = new OrderRequest();
        reqSpirit.setCustomerName("B");
        reqSpirit.setCustomerPhone("+226 111");
        reqSpirit.setCustomerAddress("Addr2");
        reqSpirit.setDeliveryType("STANDARD");
        OrderItemRequest it2 = new OrderItemRequest();
        it2.setProductId(spiritProduct.getId());
        it2.setQuantity(1);
        reqSpirit.setItems(List.of(it2));
        reqSpirit.setDeliveryCost(new BigDecimal("0"));
        reqSpirit.setDistance(new BigDecimal("1"));
        reqSpirit.setTotalAmount(new BigDecimal("2000"));

        mockMvc.perform(post("/api/orders")
            .header("X-Store-Code", "spirit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reqSpirit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").exists());

        // As admin_sucre (store scoped), can list only sucre orders
        mockMvc.perform(get("/api/admin/orders")
            .header("X-Store-Code", "sucre")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin_sucre").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        // Spoofing header to access spirit should be forbidden
        mockMvc.perform(get("/api/admin/orders")
            .header("X-Store-Code", "spirit")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin_sucre").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "delivery", roles = {"DELIVERY"})
    void deliveryGlobal_canSeeAllOrdersAcrossStores() throws Exception {
        // Create 2 orders (one per store) through public API
        OrderRequest reqSucre = new OrderRequest();
        reqSucre.setCustomerName("A");
        reqSucre.setCustomerPhone("+226 000");
        reqSucre.setCustomerAddress("Addr");
        reqSucre.setDeliveryType("STANDARD");
        OrderItemRequest it3 = new OrderItemRequest();
        it3.setProductId(sucreProduct.getId());
        it3.setQuantity(1);
        reqSucre.setItems(List.of(it3));
        reqSucre.setDeliveryCost(new BigDecimal("0"));
        reqSucre.setDistance(new BigDecimal("1"));
        reqSucre.setTotalAmount(new BigDecimal("1000"));
        mockMvc.perform(post("/api/orders")
            .header("X-Store-Code", "sucre")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reqSucre)))
            .andExpect(status().isOk());

        OrderRequest reqSpirit = new OrderRequest();
        reqSpirit.setCustomerName("B");
        reqSpirit.setCustomerPhone("+226 111");
        reqSpirit.setCustomerAddress("Addr2");
        reqSpirit.setDeliveryType("STANDARD");
        OrderItemRequest it4 = new OrderItemRequest();
        it4.setProductId(spiritProduct.getId());
        it4.setQuantity(1);
        reqSpirit.setItems(List.of(it4));
        reqSpirit.setDeliveryCost(new BigDecimal("0"));
        reqSpirit.setDistance(new BigDecimal("1"));
        reqSpirit.setTotalAmount(new BigDecimal("2000"));
        mockMvc.perform(post("/api/orders")
            .header("X-Store-Code", "spirit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reqSpirit)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/delivery/global/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/delivery/global/orders?storeCode=spirit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));
    }
}

