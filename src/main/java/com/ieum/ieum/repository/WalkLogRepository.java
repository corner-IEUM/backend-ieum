package com.ieum.ieum.repository;

import com.ieum.ieum.domain.WalkLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//javarepository를 상속받아 WalkLog 엔티티에 대한 CRUD 기능을 제공하는 인터페이스
public interface WalkLogRepository extends JpaRepository<WalkLog, Long> {

    // 메서드 이름 규칙: findBy + [필드명] + OrderBy + [필드명] + Desc
    //"SELECT * FROM walk_log WHERE user_id = ? ORDER BY created_at DESC" 쿼리를 자동 생성함
    //userId를 기준으로 보행 기록을 조회하는 메서드
    
    // 탭에서 유저별 기록을 최신순으로 보여줄때 쓰기 가능 
    List<WalkLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}