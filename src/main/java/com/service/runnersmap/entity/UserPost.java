package com.service.runnersmap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "userPost")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserPost {

  @EmbeddedId
  private UserPostPK id; // 복합 키(postId, userId)

  // 테이블간 관계를 지정하기 위해 추가
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "users_id", insertable = false, updatable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "posts_id", insertable = false, updatable = false)
  private Post post;

  @Column(nullable = true)
  private Boolean valid_yn; // 유효여부(탈퇴, 강퇴여부)

  @Column(nullable = true)
  private Double totalDistance; // 달린 거리

  @Column(nullable = true)
  private LocalDateTime actualStartTime; //(실제)출발시간

  @Column(nullable = true)
  private LocalDateTime actualEndTime; //(실제)도착시간

  @Column(nullable = true)
  private Duration runningDuration; // 소요시간

  @CreatedDate
  private LocalDateTime createdDateTime;
  @LastModifiedDate
  private LocalDateTime updatedDateTime;

}
