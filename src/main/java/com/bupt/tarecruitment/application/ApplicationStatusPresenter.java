package com.bupt.tarecruitment.application;

public final class ApplicationStatusPresenter {
    private ApplicationStatusPresenter() {
    }

    public static String toDisplayText(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case SHORTLISTED -> "Shortlisted";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
            case WITHDRAWN -> "Withdrawn";
        };
    }
}
