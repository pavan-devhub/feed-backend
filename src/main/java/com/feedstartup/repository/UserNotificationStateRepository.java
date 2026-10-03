package com.feedstartup.repository;

import com.feedstartup.model.UserNotificationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationStateRepository extends JpaRepository<UserNotificationState, Long> {
}
