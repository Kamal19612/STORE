package com.sucrestore.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sucrestore.api.entity.Slider;

@Repository
public interface SliderRepository extends JpaRepository<Slider, Long> {

    List<Slider> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Slider> findAllByOrderByDisplayOrderAsc();
}
