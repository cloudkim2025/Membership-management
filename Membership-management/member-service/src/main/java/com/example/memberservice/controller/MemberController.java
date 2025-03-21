package com.example.memberservice.controller;

import com.example.memberservice.service.MemberService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public Mono<String> createMember(@RequestBody Object memberData) {
        return memberService.createMember(memberData);
    }

    @GetMapping("/{id}")
    public Mono<String> getMember(@PathVariable Long id) {
        return memberService.getMember(id);
    }

    @PutMapping("/{id}")
    public Mono<String> updateMember(@PathVariable Long id, @RequestBody Object memberData) {
        return memberService.updateMember(id, memberData);
    }

    @DeleteMapping("/{id}")
    public Mono<String> deleteMember(@PathVariable Long id) {
        return memberService.deleteMember(id);
    }
}
