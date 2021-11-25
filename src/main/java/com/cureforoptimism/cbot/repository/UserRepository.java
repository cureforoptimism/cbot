package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.User;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByDiscordId(Long discordId);

  Optional<User> findByDiscordIdAndServer_DiscordId(Long userId, Long guildId);

  Set<User> findByServer_DiscordId(Long guildId);
}
