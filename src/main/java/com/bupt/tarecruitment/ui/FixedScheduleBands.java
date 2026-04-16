package com.bupt.tarecruitment.ui;

import com.bupt.tarecruitment.common.schedule.ScheduleSlot;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提供 applicant 与 MO 端统一使用的固定课表时间段。
 */
final class FixedScheduleBands {
    static final List<String> WEEKDAY_CODES = List.of("MON", "TUE", "WED", "THU", "FRI");

    private static final Map<String, String> WEEKDAY_LABELS = Map.of(
        "MON", "Mon",
        "TUE", "Tue",
        "WED", "Wed",
        "THU", "Thu",
        "FRI", "Fri"
    );

    private static final List<TimeBand> TIME_BANDS = List.of(
        new TimeBand("8:00-9:35", LocalTime.of(8, 0), LocalTime.of(9, 35)),
        new TimeBand("9:50-11:25", LocalTime.of(9, 50), LocalTime.of(11, 25)),
        new TimeBand("11:40-12:25", LocalTime.of(11, 40), LocalTime.of(12, 25)),
        new TimeBand("13:00-14:35", LocalTime.of(13, 0), LocalTime.of(14, 35)),
        new TimeBand("14:45-16:25", LocalTime.of(14, 45), LocalTime.of(16, 25)),
        new TimeBand("16:35-18:10", LocalTime.of(16, 35), LocalTime.of(18, 10)),
        new TimeBand("18:30-19:15", LocalTime.of(18, 30), LocalTime.of(19, 15))
    );

    private FixedScheduleBands() {
    }

    static List<TimeBand> timeBands() {
        return TIME_BANDS;
    }

    static List<String> timeBandLabels() {
        return TIME_BANDS.stream().map(TimeBand::label).toList();
    }

    static Optional<TimeBand> bandForLabel(String label) {
        return TIME_BANDS.stream()
            .filter(band -> band.label().equals(label))
            .findFirst();
    }

    static Optional<TimeBand> bandForSlot(String rawSlot) {
        try {
            ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
            return TIME_BANDS.stream()
                .filter(band -> band.start().equals(slot.startTime()) && band.end().equals(slot.endTime()))
                .findFirst();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    static String toSlotValue(String dayCode, TimeBand band) {
        return dayCode + "-" + band.start().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            + "-" + band.end().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    static String formatSlotForDisplay(String rawSlot) {
        try {
            ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
            String dayLabel = WEEKDAY_LABELS.getOrDefault(slot.dayCode(), slot.dayCode());
            Optional<TimeBand> band = bandForSlot(rawSlot);
            if (band.isPresent()) {
                return dayLabel + " " + band.get().label();
            }
            return dayLabel + " " + slot.format().substring(slot.dayCode().length() + 1);
        } catch (IllegalArgumentException exception) {
            return rawSlot;
        }
    }

    static String formatScheduleList(List<String> slots) {
        if (slots == null || slots.isEmpty()) {
            return "(schedule not listed)";
        }
        return slots.stream()
            .map(FixedScheduleBands::formatSlotForDisplay)
            .toList()
            .stream()
            .reduce((left, right) -> left + ", " + right)
            .orElse("(schedule not listed)");
    }

    static List<String> normalizeToFixedBandSlots(List<String> rawSlots) {
        if (rawSlots == null || rawSlots.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String rawSlot : rawSlots) {
            normalized.addAll(expandSlotToFixedBands(rawSlot));
        }

        return normalized.stream()
            .sorted(slotComparator())
            .toList();
    }

    static Map<String, String> weekdayLabels() {
        return new LinkedHashMap<>(WEEKDAY_LABELS);
    }

    private static List<String> expandSlotToFixedBands(String rawSlot) {
        if (rawSlot == null || rawSlot.isBlank()) {
            return List.of();
        }

        try {
            ScheduleSlot parsedSlot = ScheduleSlot.parse(rawSlot);
            List<String> matchingBands = TIME_BANDS.stream()
                .filter(band -> parsedSlot.covers(new ScheduleSlot(parsedSlot.dayCode(), band.start(), band.end())))
                .map(band -> toSlotValue(parsedSlot.dayCode(), band))
                .toList();
            if (!matchingBands.isEmpty()) {
                return matchingBands;
            }
            return List.of(parsedSlot.format());
        } catch (IllegalArgumentException exception) {
            return List.of(rawSlot.trim());
        }
    }

    private static Comparator<String> slotComparator() {
        return Comparator
            .comparingInt(FixedScheduleBands::dayOrder)
            .thenComparingInt(FixedScheduleBands::startMinutes)
            .thenComparingInt(FixedScheduleBands::endMinutes)
            .thenComparing(String::compareToIgnoreCase);
    }

    private static int dayOrder(String rawSlot) {
        try {
            return Math.max(WEEKDAY_CODES.indexOf(ScheduleSlot.parse(rawSlot).dayCode()), WEEKDAY_CODES.size());
        } catch (IllegalArgumentException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static int startMinutes(String rawSlot) {
        try {
            ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
            return slot.startTime().getHour() * 60 + slot.startTime().getMinute();
        } catch (IllegalArgumentException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static int endMinutes(String rawSlot) {
        try {
            ScheduleSlot slot = ScheduleSlot.parse(rawSlot);
            return slot.endTime().getHour() * 60 + slot.endTime().getMinute();
        } catch (IllegalArgumentException exception) {
            return Integer.MAX_VALUE;
        }
    }

    record TimeBand(String label, LocalTime start, LocalTime end) {
        TimeBand {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label must not be blank.");
            }
            if (start == null || end == null || !end.isAfter(start)) {
                throw new IllegalArgumentException("Invalid fixed time band.");
            }
        }
    }
}
