package com.cureforoptimism.cbot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long discordId;

    @Getter
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL)
    Set<User> users;
}
