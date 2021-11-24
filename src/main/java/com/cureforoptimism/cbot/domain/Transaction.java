package com.cureforoptimism.cbot.domain;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  User user;

  @Getter String symbol;

  @Column(precision = 19, scale = 10)
  @Getter
  BigDecimal amount;

  @Column(precision = 19, scale = 10)
  @Getter
  BigDecimal purchasePrice;

  @Column(precision = 19, scale = 10)
  @Getter
  @Transient
  BigDecimal fees;

  @Getter TransactionType transactionType;

  @CreationTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "create_date")
  private Date created;

  @UpdateTimestamp
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "modify_date")
  private Date modifyDate;
}
