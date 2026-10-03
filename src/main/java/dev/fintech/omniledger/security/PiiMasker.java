package dev.fintech.omniledger.security;

public final class PiiMasker {

    private PiiMasker() {}

    public static String maskPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return pan;
        }
        String clean = pan.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        if (clean.length() == 10) {
            return "XXXXX" + clean.substring(5);
        }
        return "X".repeat(clean.length() - 4) + clean.substring(clean.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return clean.substring(0, Math.min(3, clean.length() - 4))
                + "*".repeat(clean.length() - 7 > 0 ? clean.length() - 7 : 4)
                + clean.substring(clean.length() - 4);
    }
}
