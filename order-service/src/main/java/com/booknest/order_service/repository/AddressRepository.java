package com.booknest.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.booknest.order_service.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
}