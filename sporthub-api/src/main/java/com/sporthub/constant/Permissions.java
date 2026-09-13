package com.sporthub.constant;

import java.util.List;

public final class Permissions {

    private Permissions() {
    }

    // USER / ADMIN USER MANAGEMENT
    public static final String VIEW_USERS = "VIEW_USERS";
    public static final String UPDATE_USER_STATUS = "UPDATE_USER_STATUS";
    public static final String UPDATE_USER_ROLES = "UPDATE_USER_ROLES";

    // SPORT
    public static final String CREATE_SPORT = "CREATE_SPORT";
    public static final String UPDATE_SPORT = "UPDATE_SPORT";
    public static final String DELETE_SPORT = "DELETE_SPORT";

    // VENUE / COURT
    public static final String CREATE_VENUE = "CREATE_VENUE";
    public static final String UPDATE_OWN_VENUE = "UPDATE_OWN_VENUE";
    public static final String MANAGE_OWN_COURTS = "MANAGE_OWN_COURTS";

    // BOOKING
    public static final String CREATE_BOOKING = "CREATE_BOOKING";
    public static final String VIEW_OWN_BOOKINGS = "VIEW_OWN_BOOKINGS";
    public static final String VIEW_OWNER_BOOKINGS = "VIEW_OWNER_BOOKINGS";

    // TEAM / TOURNAMENT
    public static final String CREATE_TEAM = "CREATE_TEAM";
    public static final String REGISTER_TOURNAMENT = "REGISTER_TOURNAMENT";
    public static final String MANAGE_TOURNAMENTS = "MANAGE_TOURNAMENTS";

    // PAYMENT / REVIEW / VOUCHER
    public static final String CREATE_REVIEW = "CREATE_REVIEW";
    public static final String MANAGE_VOUCHERS = "MANAGE_VOUCHERS";

    // DASHBOARD
    public static final String VIEW_ADMIN_DASHBOARD = "VIEW_ADMIN_DASHBOARD";
    public static final String VIEW_OWNER_DASHBOARD = "VIEW_OWNER_DASHBOARD";

    public static final List<String> ALL = List.of(
            VIEW_USERS,
            UPDATE_USER_STATUS,
            UPDATE_USER_ROLES,

            CREATE_SPORT,
            UPDATE_SPORT,
            DELETE_SPORT,

            CREATE_VENUE,
            UPDATE_OWN_VENUE,
            MANAGE_OWN_COURTS,

            CREATE_BOOKING,
            VIEW_OWN_BOOKINGS,
            VIEW_OWNER_BOOKINGS,

            CREATE_TEAM,
            REGISTER_TOURNAMENT,
            MANAGE_TOURNAMENTS,

            CREATE_REVIEW,
            MANAGE_VOUCHERS,

            VIEW_ADMIN_DASHBOARD,
            VIEW_OWNER_DASHBOARD
    );
}