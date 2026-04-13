package com.bupt.tarecruitment.admin;

import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.ApplicationStatus;
import com.bupt.tarecruitment.application.JobApplication;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.common.schedule.ScheduleSlot;
import com.bupt.tarecruitment.job.JobPosting;
import com.bupt.tarecruitment.job.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计已录用 TA 的工作量、排期冲突和风险信息。
 */
public final class AdminWorkloadService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public AdminWorkloadService(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        UserRepository userRepository
    ) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public List<WorkloadSummary> listAcceptedTaWorkloads(int weeklyHourLimit) {
        // US14: 只统计状态为 ACCEPTED 的 TA 录用记录
        validateWeeklyHourLimit(weeklyHourLimit);

        // 先按 applicantUserId 分组，后面每个 TA 出一份 WorkloadSummary
        Map<String, List<JobApplication>> acceptedApplicationsByApplicant = applicationRepository.findAll().stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .collect(Collectors.groupingBy(JobApplication::applicantUserId));

        return acceptedApplicationsByApplicant.entrySet().stream()
            .map(entry -> buildSummary(entry.getKey(), entry.getValue(), weeklyHourLimit))
            .sorted(Comparator.comparing(WorkloadSummary::applicantUserId))
            .toList();
    }

    public Optional<WorkloadSummary> getAcceptedTaWorkload(String applicantUserId, int weeklyHourLimit) {
        if (applicantUserId == null || applicantUserId.isBlank()) {
            throw new IllegalArgumentException("applicantUserId must not be blank.");
        }

        validateWeeklyHourLimit(weeklyHourLimit);

        // 单独取某个 TA 的 ACCEPTED 记录
        List<JobApplication> acceptedApplications = applicationRepository.findByApplicantUserId(applicantUserId.trim()).stream()
            .filter(application -> application.status() == ApplicationStatus.ACCEPTED)
            .toList();

        if (acceptedApplications.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(buildSummary(applicantUserId.trim(), acceptedApplications, weeklyHourLimit));
    }

    private WorkloadSummary buildSummary(String applicantUserId, List<JobApplication> acceptedApplications, int weeklyHourLimit) {
        List<AcceptedAssignment> acceptedAssignments = acceptedApplications.stream()
            .map(this::toAcceptedAssignment)
            .sorted(Comparator.comparing(AcceptedAssignment::jobId))
            .toList();

        // 汇总每个 TA 的总周工时
        int totalWeeklyHours = acceptedAssignments.stream()
            .mapToInt(AcceptedAssignment::weeklyHours)
            .sum();

        // 解析排期，找出冲突或格式非法的数据
        ScheduleAnalysis scheduleAnalysis = analyzeSchedules(acceptedAssignments);
        String applicantDisplayName = userRepository.findByUserId(applicantUserId)
            .map(UserAccount::displayName)
            .filter(displayName -> !displayName.isBlank())
            .orElse(applicantUserId);

        return new WorkloadSummary(
            applicantUserId,
            applicantDisplayName,
            totalWeeklyHours,
            acceptedAssignments,
            scheduleAnalysis.conflicts(),
            scheduleAnalysis.invalidScheduleEntries(),
            totalWeeklyHours > weeklyHourLimit,
            !scheduleAnalysis.conflicts().isEmpty(),
            !scheduleAnalysis.invalidScheduleEntries().isEmpty()
        );
    }

    private AcceptedAssignment toAcceptedAssignment(JobApplication application) {
        // 每条 ACCEPTED 申请会映射成一个可展示的 assignment（含岗位信息与排期）
        JobPosting jobPosting = jobRepository.findByJobId(application.jobId())
            .orElseThrow(() -> new IllegalStateException("Accepted application references unknown jobId: " + application.jobId()));

        return new AcceptedAssignment(
            jobPosting.jobId(),
            jobPosting.title(),
            jobPosting.moduleOrActivity(),
            jobPosting.weeklyHours(),
            jobPosting.scheduleSlots()
        );
    }

    private ScheduleAnalysis analyzeSchedules(List<AcceptedAssignment> acceptedAssignments) {
        Set<WorkloadConflict> conflicts = new LinkedHashSet<>();
        Set<String> invalidScheduleEntries = new LinkedHashSet<>();
        Map<String, List<ParsedScheduleSlot>> parsedSlotsByJob = new LinkedHashMap<>();

        for (AcceptedAssignment assignment : acceptedAssignments) {
            List<ParsedScheduleSlot> parsedSlots = new ArrayList<>();
            for (String scheduleSlotRaw : assignment.scheduleSlots()) {
                try {
                    // 排期格式正确就解析，格式错误就记录为 invalid
                    parsedSlots.add(new ParsedScheduleSlot(scheduleSlotRaw, ScheduleSlot.parse(scheduleSlotRaw)));
                } catch (IllegalArgumentException exception) {
                    // 这里不抛错，避免一个坏数据导致整个页面挂掉
                    invalidScheduleEntries.add(
                        assignment.jobId()
                            + " | "
                            + assignment.title()
                            + " | Invalid schedule slot: "
                            + scheduleSlotRaw
                    );
                }
            }
            parsedSlotsByJob.put(assignment.jobId(), parsedSlots);
        }

        for (int leftIndex = 0; leftIndex < acceptedAssignments.size(); leftIndex++) {
            AcceptedAssignment leftAssignment = acceptedAssignments.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < acceptedAssignments.size(); rightIndex++) {
                AcceptedAssignment rightAssignment = acceptedAssignments.get(rightIndex);
                // 两两比较排期，找出重叠时间段
                conflicts.addAll(findConflictsBetween(
                    leftAssignment,
                    parsedSlotsByJob.getOrDefault(leftAssignment.jobId(), List.of()),
                    rightAssignment,
                    parsedSlotsByJob.getOrDefault(rightAssignment.jobId(), List.of())
                ));
            }
        }

        List<WorkloadConflict> sortedConflicts = conflicts.stream()
            .sorted(Comparator
                .comparing(WorkloadConflict::overlapSlot)
                .thenComparing(WorkloadConflict::jobIdA)
                .thenComparing(WorkloadConflict::jobIdB))
            .toList();

        return new ScheduleAnalysis(sortedConflicts, invalidScheduleEntries.stream().sorted().toList());
    }

    private List<WorkloadConflict> findConflictsBetween(
        AcceptedAssignment leftAssignment,
        List<ParsedScheduleSlot> leftSlots,
        AcceptedAssignment rightAssignment,
        List<ParsedScheduleSlot> rightSlots
    ) {
        List<WorkloadConflict> conflicts = new ArrayList<>();

        for (ParsedScheduleSlot leftParsedSlot : leftSlots) {
            for (ParsedScheduleSlot rightParsedSlot : rightSlots) {
                ScheduleSlot leftSlot = leftParsedSlot.slot();
                ScheduleSlot rightSlot = rightParsedSlot.slot();
                // 这里是排期冲突的核心判断：只要时间段重叠，就认为冲突
                if (!leftSlot.overlaps(rightSlot)) {
                    continue;
                }

                ScheduleSlot overlapSlot = leftSlot.overlapWith(rightSlot);
                // 记录冲突的两门课 + 重叠时间段
                conflicts.add(new WorkloadConflict(
                    leftAssignment.jobId(),
                    leftAssignment.title(),
                    rightAssignment.jobId(),
                    rightAssignment.title(),
                    overlapSlot.format()
                ));
            }
        }

        return conflicts;
    }

    private void validateWeeklyHourLimit(int weeklyHourLimit) {
        // 管理员输入的周工时上限必须是正数
        if (weeklyHourLimit <= 0) {
            throw new IllegalArgumentException("weeklyHourLimit must be greater than 0.");
        }
    }

    private record ParsedScheduleSlot(String rawValue, ScheduleSlot slot) {
        private ParsedScheduleSlot {
            Objects.requireNonNull(rawValue);
            Objects.requireNonNull(slot);
        }
    }

    private record ScheduleAnalysis(
        List<WorkloadConflict> conflicts,
        List<String> invalidScheduleEntries
    ) {
        private ScheduleAnalysis {
            Objects.requireNonNull(conflicts);
            Objects.requireNonNull(invalidScheduleEntries);
        }
    }
}
