package com.solomonidis.familyhub.member;

/** Compact member reference for embedding in other responses ("added by", "assigned to", …). */
public record MemberSummary(Long id, String name, String color) {

    /** Null-safe: returns null when there is no member (e.g. seed data). Call inside a transaction for lazy refs. */
    public static MemberSummary of(Member member) {
        return member == null ? null : new MemberSummary(member.getId(), member.getName(), member.getColor());
    }
}
