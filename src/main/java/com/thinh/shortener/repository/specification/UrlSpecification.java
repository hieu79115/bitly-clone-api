package com.thinh.shortener.repository.specification;

import com.thinh.shortener.domain.entity.Tag;
import com.thinh.shortener.domain.entity.Url;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UrlSpecification {

    public static Specification<Url> filterUrls(Long userId, Long tagId, String search, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory filter: userId
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            // 2. Tag filter
            if (tagId != null) {
                Join<Url, Tag> tagJoin = root.join("tags");
                predicates.add(cb.equal(tagJoin.get("id"), tagId));
                if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                    query.distinct(true);
                }
            }

            // 3. Search query filter (title, shortCode, originalUrl)
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("shortCode")), pattern);
                Predicate urlLike = cb.like(cb.lower(root.get("originalUrl")), pattern);
                predicates.add(cb.or(titleLike, codeLike, urlLike));
            }

            // 4. Status filter: "active" vs "expired"
            if (StringUtils.hasText(status)) {
                LocalDateTime now = LocalDateTime.now();
                String statusLower = status.trim().toLowerCase();
                if ("active".equals(statusLower)) {
                    // Active: expiresAt IS NULL OR expiresAt > now
                    Predicate neverExpires = cb.isNull(root.get("expiresAt"));
                    Predicate notExpiredYet = cb.greaterThan(root.get("expiresAt"), now);
                    predicates.add(cb.or(neverExpires, notExpiredYet));
                } else if ("expired".equals(statusLower)) {
                    // Expired: expiresAt IS NOT NULL AND expiresAt <= now
                    Predicate hasExpiry = cb.isNotNull(root.get("expiresAt"));
                    Predicate isExpired = cb.lessThanOrEqualTo(root.get("expiresAt"), now);
                    predicates.add(cb.and(hasExpiry, isExpired));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
