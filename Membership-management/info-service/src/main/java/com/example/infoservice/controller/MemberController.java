package com.example.infoservice.controller;

import com.example.infoservice.domain.Member;
import com.example.infoservice.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원 생성: POST /members
    @PostMapping
    public ResponseEntity<Member> createMember(@Validated @RequestBody Member member) {
        Member savedMember = memberService.createMember(member);
        return new ResponseEntity<>(savedMember, HttpStatus.CREATED);
    }

    // 단일 회원 조회: GET /members/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMember(@PathVariable Long id) {
        // 서비스에서 회원을 찾지 못하면 MemberNotFoundException이 발생하고, GlobalExceptionHandler에서 처리됩니다.
        Member member = memberService.getMember(id);
        return ResponseEntity.ok(member);
    }

    // 전체 회원 조회: GET /members
    @GetMapping
    public ResponseEntity<Iterable<Member>> getAllMembers() {
        Iterable<Member> members = memberService.getAllMembers();
        return ResponseEntity.ok(members);
    }

    // 회원 정보 수정: PUT /members/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Member> updateMember(@PathVariable Long id, @RequestBody Member member) {
        // 회원이 없을 경우 MemberNotFoundException이 발생합니다.
        Member updatedMember = memberService.updateMember(id, member);
        return ResponseEntity.ok(updatedMember);
    }

    // 회원 삭제: DELETE /members/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        // 회원이 없으면 MemberNotFoundException 발생
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}
