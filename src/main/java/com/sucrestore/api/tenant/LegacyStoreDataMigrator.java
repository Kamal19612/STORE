package com.sucrestore.api.tenant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.entity.Category;
import com.sucrestore.api.entity.Order;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.entity.Slider;
import com.sucrestore.api.entity.Store;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.AppSettingRepository;
import com.sucrestore.api.repository.CategoryRepository;
import com.sucrestore.api.repository.OrderRepository;
import com.sucrestore.api.repository.ProductRepository;
import com.sucrestore.api.repository.SliderRepository;
import com.sucrestore.api.repository.StoreRepository;
import com.sucrestore.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-shot migrator (best-effort) to attach existing rows to default store "sucre".
 * This avoids breaking the current production DB when adding store_id columns.
 *
 * It is idempotent: only rows with store_id NULL are updated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyStoreDataMigrator implements ApplicationRunner {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SliderRepository sliderRepository;
    private final AppSettingRepository appSettingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Store sucre = storeRepository.findByCode(StoreResolverService.DEFAULT_STORE_CODE)
            .orElse(null);
        if (sucre == null) {
            log.warn("[TENANT] Default store not found, skip legacy migration");
            return;
        }

        long p = attachProducts(sucre);
        long o = attachOrders(sucre);
        long c = attachCategories(sucre);
        long s = attachSliders(sucre);
        long a = attachAppSettings(sucre);
        long u = attachUsers(sucre);

        if (p + o + c + s + a + u > 0) {
            log.info("[TENANT] Legacy migration attached to store='{}': products={}, orders={}, categories={}, sliders={}, settings={}, users={}",
                sucre.getCode(), p, o, c, s, a, u);
        }
    }

    private long attachProducts(Store store) {
        long count = 0;
        for (Product x : productRepository.findAll()) {
            if (x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }

    private long attachOrders(Store store) {
        long count = 0;
        for (Order x : orderRepository.findAll()) {
            if (x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }

    private long attachCategories(Store store) {
        long count = 0;
        for (Category x : categoryRepository.findAll()) {
            if (x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }

    private long attachSliders(Store store) {
        long count = 0;
        for (Slider x : sliderRepository.findAll()) {
            if (x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }

    private long attachAppSettings(Store store) {
        long count = 0;
        for (AppSetting x : appSettingRepository.findAll()) {
            if (x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }

    private long attachUsers(Store store) {
        long count = 0;
        for (User x : userRepository.findAll()) {
            // Delivery users are global -> leave NULL store
            boolean isDelivery = x.getRole() == User.Role.DELIVERY_AGENT || x.getRole() == User.Role.DELIVERY;
            if (!isDelivery && x.getStore() == null) {
                x.setStore(store);
                count++;
            }
        }
        return count;
    }
}

