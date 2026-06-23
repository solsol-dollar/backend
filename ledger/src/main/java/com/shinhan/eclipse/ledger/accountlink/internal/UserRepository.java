package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, Long> {
}
