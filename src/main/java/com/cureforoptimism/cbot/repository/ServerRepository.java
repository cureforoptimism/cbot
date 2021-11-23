package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    Optional<Server> findByDiscordId(Long discordId);
}
