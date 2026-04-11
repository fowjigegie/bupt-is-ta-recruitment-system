package com.bupt.tarecruitment.application;

public final class ApplicationStatusPresenter {
    private ApplicationStatusPresenter() {
    }

    // US06: 把内部状态枚举转成 UI 上更适合展示给用户的文本。
    // 这样页面层就不需要自己写一堆 switch。
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
