package com.ieum.ieum.repository;

import com.ieum.ieum.domain.Navigation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NavigationRepository extends JpaRepository<Navigation, Long> {
    List<Navigation> findByUserIdOrderByStartTimeDesc(Long userId);  // 내 길찾기 기록용
}