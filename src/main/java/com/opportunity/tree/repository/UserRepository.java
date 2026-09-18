package com.opportunity.tree.repository;

import com.opportunity.tree.domain.User;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE, unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "select u from User u where u.activated = true and (" +
            "lower(u.login) like lower(concat('%', :q, '%')) or " +
            "lower(coalesce(u.firstName, '')) like lower(concat('%', :q, '%')) or " +
            "lower(coalesce(u.lastName, '')) like lower(concat('%', :q, '%')))"
    )
    Page<User> searchByLoginOrName(@org.springframework.data.repository.query.Param("q") String q, Pageable pageable);
}
