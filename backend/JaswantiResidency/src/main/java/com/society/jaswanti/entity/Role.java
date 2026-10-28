package com.society.jaswanti.entity;

public enum Role {
    ADMIN,
    MASTER_ADMIN,
    /** Office staff: same as ADMIN for society, residents and finance, but no helpdesk or announcements. */
    OFFICE_ASSISTANT,
    RESIDENT;

    /** Society staff (anyone who works in the admin area), as opposed to residents. */
    public boolean isStaff() {
        return this != RESIDENT;
    }
}
