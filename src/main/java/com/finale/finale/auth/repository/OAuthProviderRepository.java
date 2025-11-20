package com.finale.finale.auth.repository;

import com.finale.finale.auth.domain.OAuthProvider;
import com.finale.finale.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthProviderRepository extends JpaRepository<OAuthProvider, Long> {
    Optional<OAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OAuthProvider> findByUser(User user);
}
