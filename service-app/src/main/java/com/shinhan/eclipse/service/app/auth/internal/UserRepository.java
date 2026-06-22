package com.shinhan.eclipse.service.app.auth.internal;

import com.shinhan.eclipse.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}