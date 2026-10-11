package com.campusfind.repository;

import com.campusfind.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Leaderboard: Top users ordered by karmaPoints in descending order
     */
    List<User> findTop10ByBannedFalseOrderByKarmaPointsDesc();

    List<User> findTop20ByBannedFalseOrderByKarmaPointsDesc();

    Page<User> findByBannedFalseOrderByKarmaPointsDesc(Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@org.springframework.data.repository.query.Param("search") String search, Pageable pageable);
}
