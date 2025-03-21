package com.example.infoservice.repository;

import com.example.infoservice.domain.Member;
import org.springframework.data.repository.CrudRepository;

public interface MemberRepository extends CrudRepository<Member, Long> {
}
