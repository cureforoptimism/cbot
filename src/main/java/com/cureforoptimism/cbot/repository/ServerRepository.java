package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Server;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server, Long> {
  Optional<Server> findByDiscordId(Long discordId);
}
