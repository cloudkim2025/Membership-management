package com.example.infoservice.service;

import com.example.infoservice.domain.Member;
import com.example.infoservice.exception.MemberNotFoundException;
import com.example.infoservice.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 회원 생성
    public Member createMember(Member member) {
        return memberRepository.save(member);
    }

    // 단일 회원 조회
    public Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    // 전체 회원 조회
    public Iterable<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // 회원 정보 수정
    public Member updateMember(Long id, Member updatedMember) {
        return memberRepository.findById(id)
                .map(existingMember -> {
                    existingMember.setName(updatedMember.getName());
                    existingMember.setContact(updatedMember.getContact());
                    return memberRepository.save(existingMember);
                })
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    // 회원 삭제
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException(id);
        }
        memberRepository.deleteById(id);
    }
}
