package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDiscordId(Long discordId);
}
