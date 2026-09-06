package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByIdAndUserId(Long id, Long userId);

    Page<Url> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT u FROM Url u JOIN u.tags t WHERE u.user.id = :userId AND t.id = :tagId")
    Page<Url> findByUserIdAndTagId(@Param("userId") Long userId, @Param("tagId") Long tagId, Pageable pageable);
}
