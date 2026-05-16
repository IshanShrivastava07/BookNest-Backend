package com.booknest.order_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booknest.order_service.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.items i "
            + "WHERE o.userId = :userId AND i.bookId = :bookId AND o.orderStatus <> 'CANCELLED'")
    boolean userHasPurchasedBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);
}
