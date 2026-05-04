package com.sucrestore.api.tenant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sucrestore.api.entity.Store;
import com.sucrestore.api.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ensures default stores exist.
 * Backward compatibility: all existing data will be attached to store "sucre" by a separate migrator.
 */
@Component
@RequiredArgsConstructor
public class StoreBootstrap implements ApplicationRunner {

    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensure("sucre", "SUCRE STORE");
        ensure("spirit", "SPIRIT STORE");
    }

    private void ensure(String code, String name) {
        storeRepository.findByCode(code).orElseGet(() -> storeRepository.save(
            Store.builder()
                .code(code)
                .name(name)
                .domain(null)
                .build()
        ));
    }
}

