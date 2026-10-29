package com.smartschedule.dto;

public class PolicyValidationResult {
    private boolean valid;             // true if no blocking error
    private boolean violated;          // true if exceeds weekly or monthly policy limit (requires admin escalation)
    private boolean duplicate;         // true if identical or overlapping with an existing active request
    private String message;            // human readable message
    private int weeklyRequestCount;    // total requests in the Monday-to-Sunday week
    private int weeklyLimit;           // configured weekly request limit (e.g. 2)
    private double monthlyLeaveDays;   // total leave days in the calendar month
    private int monthlyLimit;          // configured monthly leave day limit (e.g. 7)

    public PolicyValidationResult() {}

    public PolicyValidationResult(boolean valid, boolean violated, boolean duplicate, String message,
                                  int weeklyRequestCount, int weeklyLimit, double monthlyLeaveDays, int monthlyLimit) {
        this.valid = valid;
        this.violated = violated;
        this.duplicate = duplicate;
        this.message = message;
        this.weeklyRequestCount = weeklyRequestCount;
        this.weeklyLimit = weeklyLimit;
        this.monthlyLeaveDays = monthlyLeaveDays;
        this.monthlyLimit = monthlyLimit;
    }

    public static PolicyValidationResult ok(int weeklyCount, int weeklyLimit, double monthlyDays, int monthlyLimit) {
        PolicyValidationResult res = new PolicyValidationResult();
        res.setValid(true);
        res.setViolated(false);
        res.setDuplicate(false);
        res.setMessage("Request conforms to institutional policy.");
        res.setWeeklyRequestCount(weeklyCount);
        res.setWeeklyLimit(weeklyLimit);
        res.setMonthlyLeaveDays(monthlyDays);
        res.setMonthlyLimit(monthlyLimit);
        return res;
    }

    public static PolicyValidationResult violation(String message, int weeklyCount, int weeklyLimit, double monthlyDays, int monthlyLimit) {
        PolicyValidationResult res = new PolicyValidationResult();
        res.setValid(true); // Still submittable for admin review/escalation
        res.setViolated(true);
        res.setDuplicate(false);
        res.setMessage(message);
        res.setWeeklyRequestCount(weeklyCount);
        res.setWeeklyLimit(weeklyLimit);
        res.setMonthlyLeaveDays(monthlyDays);
        res.setMonthlyLimit(monthlyLimit);
        return res;
    }

    public static PolicyValidationResult duplicate(String message) {
        PolicyValidationResult res = new PolicyValidationResult();
        res.setValid(false);
        res.setViolated(false);
        res.setDuplicate(true);
        res.setMessage(message);
        return res;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isViolated() {
        return this.violated;
    }

    public void setViolated(boolean violated) {
        this.violated = violated;
    }

    public boolean isDuplicate() {
        return this.duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getWeeklyRequestCount() {
        return this.weeklyRequestCount;
    }

    public void setWeeklyRequestCount(int weeklyRequestCount) {
        this.weeklyRequestCount = weeklyRequestCount;
    }

    public int getWeeklyLimit() {
        return this.weeklyLimit;
    }

    public void setWeeklyLimit(int weeklyLimit) {
        this.weeklyLimit = weeklyLimit;
    }

    public double getMonthlyLeaveDays() {
        return this.monthlyLeaveDays;
    }

    public void setMonthlyLeaveDays(double monthlyLeaveDays) {
        this.monthlyLeaveDays = monthlyLeaveDays;
    }

    public int getMonthlyLimit() {
        return this.monthlyLimit;
    }

    public void setMonthlyLimit(int monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
