package com.threadly.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

	@Query("""
			select i from UserIdentity i
			join fetch i.user
			where i.provider = :provider and i.providerUserId = :providerUserId
			""")
	Optional<UserIdentity> findByProviderAndSubject(
			@Param("provider") IdentityProvider provider,
			@Param("providerUserId") String providerUserId);

	boolean existsByUserIdAndProvider(Long userId, IdentityProvider provider);
}
