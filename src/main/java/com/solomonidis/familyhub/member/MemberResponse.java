package com.solomonidis.familyhub.member;

public record MemberResponse(Long id, String name, String username, String color, boolean active) {

    public static MemberResponse of(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getUsername(),
            member.getColor(), member.isActive());
    }
}
