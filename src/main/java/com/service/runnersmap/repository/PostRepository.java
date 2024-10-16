package com.service.runnersmap.repository;

import com.service.runnersmap.entity.Post;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(value =
          "SELECT * " +
          "FROM post p " +
          "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(p.lat)) " +
          "* cos(radians(p.lng) - radians(:lng)) " +
          "+ sin(radians(:lat)) * sin(radians(p.lat)))) < 2 " +
          "AND (:gender IS NULL OR p.gender = :gender) " +
          "AND (:paceMinStart IS NULL OR (p.pace_min + p.pace_sec / 60) >= :paceMinStart) " +
          "AND (:paceMinEnd IS NULL OR (p.pace_min + p.pace_sec / 60) <= :paceMinEnd) " +
          "AND (:distanceStart IS NULL OR p.distance >= :distanceStart) " +
          "AND (:distanceEnd IS NULL OR p.distance <= :distanceEnd) " +
          "AND (:startDate IS NULL OR (p.start_date_time BETWEEN :startDate AND DATE_ADD(:startDate, INTERVAL 1 DAY))) " +
          "AND (:startTime IS NULL OR TIME_FORMAT(p.start_date_time, '%H%i') = :startTime) " +
          "AND (:limitMemberCnt IS NULL OR p.limit_member_cnt = :limitMemberCnt)" +
          "AND p.start_date_time >= CURRENT_TIMESTAMP " +
          "AND p.departure_yn = false " +
          "AND p.arrive_yn = false " +
          "ORDER BY p.start_date_time ASC ",
      countQuery =
              "SELECT COUNT(*) "+
              "FROM post p " +
              "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(p.lat)) " +
              "* cos(radians(p.lng) - radians(:lng)) " +
              "+ sin(radians(:lat)) * sin(radians(p.lat)))) < 2 " +
              "AND (:gender IS NULL OR p.gender = :gender) " +
              "AND (:paceMinStart IS NULL OR (p.pace_min + p.pace_sec / 60) >= :paceMinStart) " +
              "AND (:paceMinEnd IS NULL OR (p.pace_min + p.pace_sec / 60) <= :paceMinEnd) " +
              "AND (:distanceStart IS NULL OR p.distance >= :distanceStart) " +
              "AND (:distanceEnd IS NULL OR p.distance <= :distanceEnd) " +
              "AND (:startDate IS NULL OR (p.start_date_time BETWEEN :startDate AND DATE_ADD(:startDate, INTERVAL 1 DAY))) "+
              "AND (:startTime IS NULL OR TIME_FORMAT(p.start_date_time, '%H%i') = :startTime) " +
              "AND (:limitMemberCnt IS NULL OR p.limit_member_cnt = :limitMemberCnt)" +
              "AND p.start_date_time >= CURRENT_TIMESTAMP " +
              "AND p.departure_yn = false " +
              "AND p.arrive_yn = false ",
      nativeQuery = true)
  Page<Post> findAllWithin2Km(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("gender") String gender,
      @Param("paceMinStart") Integer paceMinStart,
      @Param("paceMinEnd") Integer paceMinEnd,
      @Param("distanceStart") Long distanceStart,
      @Param("distanceEnd") Long distanceEnd,
      @Param("startDate") LocalDate startDate,
      @Param("startTime") String startTime,
      @Param("limitMemberCnt") Integer limitMemberCnt,
      Pageable pageable
  );


  @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
      "FROM Post p " +
      "WHERE p.admin.id = :adminId " +
      "AND (p.arriveYn IS NULL OR p.arriveYn = false)")
  boolean existsByAdminIdAndArriveYnFalse(
      @Param("adminId") Long adminId);


  @Query(" SELECT p " +
      "FROM UserPost up " +
      "JOIN up.post p " +
      "WHERE up.id.userId = :userId " +
      "AND up.valid_yn = true " +
      "ORDER BY p.startDateTime DESC")
  List<Post> findAllByUserId(@Param("userId") Long userId);
}
