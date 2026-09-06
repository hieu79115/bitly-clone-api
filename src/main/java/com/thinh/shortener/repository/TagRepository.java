package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUserId(Long userId);

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    // Check if the tag name already exists for this user (used during creation)
    boolean existsByUserIdAndName(Long userId, String name);

    // Check if the tag name conflicts with another tag belonging to the same user (used during rename updates)
    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);

    @Query("SELECT t FROM Tag t WHERE t.id IN :ids AND t.user.id = :userId")
    Set<Tag> findByIdInAndUserId(@Param("ids") Set<Long> ids, @Param("userId") Long userId);
}
