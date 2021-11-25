package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.repository.ServerRepository;
import com.cureforoptimism.cbot.repository.UserRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class UserService {
  private final UserRepository userRepository;
  private final ServerRepository serverRepository;

  // This should be done by native query joins, but whatever for now
  public Optional<User> findByDiscordIdAndServerId(Long discordId, Long guildId) {
    return userRepository.findByDiscordIdAndServer_DiscordId(discordId, guildId);
  }
}
