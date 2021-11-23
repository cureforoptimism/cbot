package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByDiscordId(Long discordId);
}
