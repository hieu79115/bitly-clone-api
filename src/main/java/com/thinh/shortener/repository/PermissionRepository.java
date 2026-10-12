package com.thinh.shortener.repository;

import com.thinh.shortener.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);

    List<Permission> findByModule(String module);

    List<Permission> findByNameIn(Collection<String> names);

    boolean existsByName(String name);
}
