package com.service.runnersmap.repository;

import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.entity.UserPostPK;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserPostRepository extends JpaRepository<UserPost, UserPostPK> {

  boolean existsById(UserPostPK id);

  @Query("SELECT up FROM UserPost up WHERE up.id.postId = :postId AND up.valid_yn = true")
  List<UserPost> findAllByPostIdAndValidYn(@Param("postId") Long postId);

  @Query("SELECT SUM(up.totalDistance) FROM UserPost up WHERE up.id.userId = :userId AND up.valid_yn = true")
  Double findTotalDistanceByUserId(@Param("userId") Long userId);

  @Query("SELECT SUM(up.totalDistance) FROM UserPost up WHERE up.id.userId = :userId AND up.valid_yn = true AND FUNCTION('YEAR', up.actualEndTime) = :year AND FUNCTION('MONTH', up.actualEndTime) = :month")
  Double findTotalDistanceByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year,
      @Param("month") int month);

  @Query("SELECT up " +
      "FROM  UserPost up " +
      "WHERE up.id.userId = :userId " +
      "AND   up.valid_yn     = true " +
      "AND   FUNCTION('YEAR',  up.actualEndTime) = :year " +
      "AND   FUNCTION('MONTH', up.actualEndTime) = :month " +
      "ORDER BY up.actualEndTime ASC")
  List<UserPost> findAllTotalDistanceByUserId(@Param("userId") Long userId, @Param("year") int year,
      @Param("month") int month);

  @Transactional
  int deleteById_PostId(Long postId);

  @Query("SELECT CASE WHEN EXISTS ( " +
           "SELECT 1 " +
           "FROM UserPost up " +
           "WHERE up.id.postId = :postId " +
           "AND up.valid_yn = true " +
           "AND up.actualEndTime IS NULL " +
        ") THEN true ELSE false END")
  boolean existsPostIdAndValidYnTrueAndActualEndTimeIsNull(@Param("postId") Long postId);
}
