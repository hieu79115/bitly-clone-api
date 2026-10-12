package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions ORDER BY r.id ASC")
    List<Role> findAllWithPermissions();

    boolean existsByName(String name);

    List<Role> findByNameIn(Collection<String> names);
}
