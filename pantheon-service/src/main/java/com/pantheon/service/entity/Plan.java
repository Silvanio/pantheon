package com.pantheon.service.entity;

public enum Plan {

    BASIC(2),
    PRO(10),
    UNLIMITED(null);

    private final Integer projectLimit;

    Plan(Integer projectLimit) {
        this.projectLimit = projectLimit;
    }

    /**
     * Maximum number of projects an administrator may own on this plan, or {@code null} if unbounded.
     */
    public Integer getProjectLimit() {
        return projectLimit;
    }
}
