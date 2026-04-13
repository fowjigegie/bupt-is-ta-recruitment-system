package com.bupt.tarecruitment.common.schedule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示标准化的时间段排期。
 */
public record ScheduleSlot(
    String dayCode,
    LocalTime startTime,
    LocalTime endTime
) {
    private static final Pattern SLOT_PATTERN = Pattern.compile(
        "^(MON|TUE|WED|THU|FRI|SAT|SUN)-([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)$"
    );
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public ScheduleSlot {
        Objects.requireNonNull(dayCode);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Schedule slot end time must be later than start time.");
        }
    }

    public static ScheduleSlot parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("scheduleSlot must not be blank.");
        }

        Matcher matcher = SLOT_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "scheduleSlot must use the format DAY-HH:MM-HH:MM, for example MON-09:00-11:00."
            );
        }

        LocalTime start = LocalTime.of(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        LocalTime end = LocalTime.of(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
        return new ScheduleSlot(matcher.group(1), start, end);
    }

    public boolean overlaps(ScheduleSlot other) {
        Objects.requireNonNull(other);
        return dayCode.equals(other.dayCode)
            && startTime.isBefore(other.endTime)
            && other.startTime.isBefore(endTime);
    }

    public ScheduleSlot overlapWith(ScheduleSlot other) {
        if (!overlaps(other)) {
            throw new IllegalArgumentException("Schedule slots do not overlap.");
        }

        LocalTime overlapStart = startTime.isAfter(other.startTime) ? startTime : other.startTime;
        LocalTime overlapEnd = endTime.isBefore(other.endTime) ? endTime : other.endTime;
        return new ScheduleSlot(dayCode, overlapStart, overlapEnd);
    }

    public String format() {
        return dayCode
            + "-"
            + startTime.format(TIME_FORMATTER)
            + "-"
            + endTime.format(TIME_FORMATTER);
    }

    @Override
    public String toString() {
        return format();
    }
}
