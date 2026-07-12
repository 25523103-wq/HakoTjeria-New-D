package com.hakotjeria.util;

import com.hakotjeria.model.Role;
import com.hakotjeria.model.Shift;
import com.hakotjeria.model.User;

/** Sesi pengguna aktif selama aplikasi berjalan. */
public final class Session {

    private static User currentUser;
    private static Shift activeShift;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Shift getActiveShift() {
        return activeShift;
    }

    public static void setActiveShift(Shift shift) {
        activeShift = shift;
    }

    public static boolean isSupervisor() {
        return currentUser != null && currentUser.getRole() == Role.SUPERVISOR;
    }

    public static boolean isStaff() {
        return currentUser != null && currentUser.getRole() == Role.STAFF;
    }

    /** Mengakhiri sesi dan menghapus seluruh state aktif (R13.3). */
    public static void clear() {
        currentUser = null;
        activeShift = null;
    }
}
