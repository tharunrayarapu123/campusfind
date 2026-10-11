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

    Page<User> findByBannedFalseOrderByKarmaPointsDesc(Pageable pageable);
}
