package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.SymbolToTickerCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface SymbolToTickerCacheRepository extends JpaRepository<SymbolToTickerCache, String> {}
