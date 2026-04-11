package UI;

import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewService;
import com.bupt.tarecruitment.applicant.ApplicantProfileIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.CvTextStorage;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
import com.bupt.tarecruitment.admin.AdminWorkloadService;
import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.ApplicationDecisionService;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplicationService;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.communication.MessageRepository;
import com.bupt.tarecruitment.communication.MessageService;
import com.bupt.tarecruitment.communication.TextFileMessageRepository;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobPostingService;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.TextFileJobRepository;
import com.bupt.tarecruitment.recommendation.MissingSkillsFeedbackService;
import com.bupt.tarecruitment.recommendation.RecommendationService;

import java.nio.file.Path;

public final class UiServices {
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ApplicantCvRepository cvRepository;
    private final CvTextStorage cvStorage;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final MessageRepository messageRepository;
    private final ApplicantProfileService profileService;
    private final ApplicantProfileIdGenerator profileIdGenerator;
    private final ApplicantCvLibraryService cvLibraryService;
    private final ApplicantCvReviewService cvReviewService;
    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;
    private final ApplicationDecisionService applicationDecisionService;
    private final AdminWorkloadService adminWorkloadService;
    private final MessageService messageService;
    private final RecommendationService recommendationService;
    private final MissingSkillsFeedbackService missingSkillsFeedbackService;

    private UiServices(
        UserRepository userRepository,
        ApplicantProfileRepository profileRepository,
        ApplicantCvRepository cvRepository,
        CvTextStorage cvStorage,
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        MessageRepository messageRepository,
        ApplicantProfileService profileService,
        ApplicantProfileIdGenerator profileIdGenerator,
        ApplicantCvLibraryService cvLibraryService,
        ApplicantCvReviewService cvReviewService,
        JobPostingService jobPostingService,
        JobApplicationService jobApplicationService,
        ApplicationDecisionService applicationDecisionService,
        AdminWorkloadService adminWorkloadService,
        MessageService messageService,
        RecommendationService recommendationService,
        MissingSkillsFeedbackService missingSkillsFeedbackService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.cvRepository = cvRepository;
        this.cvStorage = cvStorage;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.messageRepository = messageRepository;
        this.profileService = profileService;
        this.profileIdGenerator = profileIdGenerator;
        this.cvLibraryService = cvLibraryService;
        this.cvReviewService = cvReviewService;
        this.jobPostingService = jobPostingService;
        this.jobApplicationService = jobApplicationService;
        this.applicationDecisionService = applicationDecisionService;
        this.adminWorkloadService = adminWorkloadService;
        this.messageService = messageService;
        this.recommendationService = recommendationService;
        this.missingSkillsFeedbackService = missingSkillsFeedbackService;
    }

    public static UiServices create(Path dataDirectory) {
        UserRepository userRepository = new TextFileUserRepository(dataDirectory);
        ApplicantProfileRepository profileRepository = new TextFileApplicantProfileRepository(dataDirectory);
        ApplicantCvRepository cvRepository = new TextFileApplicantCvRepository(dataDirectory);
        CvTextStorage cvStorage = new TextFileCvStorage(dataDirectory);
        ApplicationRepository applicationRepository = new TextFileApplicationRepository(dataDirectory);
        JobRepository jobRepository = new TextFileJobRepository(dataDirectory);
        MessageRepository messageRepository = new TextFileMessageRepository(dataDirectory);

        // US01/US05: profile 的创建/编辑服务
        ApplicantProfileService profileService = new ApplicantProfileService(
            profileRepository,
            new ApplicantProfileValidator(),
            userRepository
        );
        // US01: 生成新的 profileId
        ApplicantProfileIdGenerator profileIdGenerator = new ApplicantProfileIdGenerator(profileRepository);
        // US02: applicant 自己维护 CV library 的主服务。
        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            cvStorage,
            userRepository
        );
        ApplicantCvReviewService cvReviewService = new ApplicantCvReviewService(
            applicationRepository,
            cvRepository,
            profileRepository,
            cvStorage
        );
        JobPostingService jobPostingService = new JobPostingService(
            jobRepository,
            new JobIdGenerator(jobRepository),
            userRepository
        );
        JobApplicationService jobApplicationService = new JobApplicationService(
            jobRepository,
            applicationRepository,
            new ApplicationIdGenerator(applicationRepository),
            profileRepository,
            cvRepository,
            userRepository
        );
        ApplicationDecisionService applicationDecisionService = new ApplicationDecisionService(
            applicationRepository,
            jobRepository,
            userRepository
        );
        AdminWorkloadService adminWorkloadService = new AdminWorkloadService(
            applicationRepository,
            jobRepository,
            userRepository
        );
        // US08: applicant 与 MO 的聊天服务。
        MessageService messageService = new MessageService(messageRepository);
        RecommendationService recommendationService = new RecommendationService(
            profileRepository,
            jobRepository
        );
        // US10: 计算“已匹配/缺失技能/覆盖率”的反馈服务
        MissingSkillsFeedbackService missingSkillsFeedbackService = new MissingSkillsFeedbackService(
            profileRepository,
            jobRepository
        );

        return new UiServices(
            userRepository,
            profileRepository,
            cvRepository,
            cvStorage,
            applicationRepository,
            jobRepository,
            messageRepository,
            profileService,
            profileIdGenerator,
            cvLibraryService,
            cvReviewService,
            jobPostingService,
            jobApplicationService,
            applicationDecisionService,
            adminWorkloadService,
            messageService,
            recommendationService,
            missingSkillsFeedbackService
        );
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public ApplicantProfileRepository profileRepository() {
        return profileRepository;
    }

    public ApplicantCvRepository cvRepository() {
        return cvRepository;
    }

    public CvTextStorage cvStorage() {
        return cvStorage;
    }

    public ApplicationRepository applicationRepository() {
        return applicationRepository;
    }

    public JobRepository jobRepository() {
        return jobRepository;
    }

    public MessageRepository messageRepository() {
        return messageRepository;
    }

    public ApplicantProfileService profileService() {
        return profileService;
    }

    public ApplicantProfileIdGenerator profileIdGenerator() {
        return profileIdGenerator;
    }

    public ApplicantCvLibraryService cvLibraryService() {
        return cvLibraryService;
    }

    public ApplicantCvReviewService cvReviewService() {
        return cvReviewService;
    }

    public JobPostingService jobPostingService() {
        return jobPostingService;
    }

    public JobApplicationService jobApplicationService() {
        return jobApplicationService;
    }

    public ApplicationDecisionService applicationDecisionService() {
        return applicationDecisionService;
    }

    public AdminWorkloadService adminWorkloadService() {
        return adminWorkloadService;
    }

    public MessageService messageService() {
        return messageService;
    }

    public RecommendationService recommendationService() {
        return recommendationService;
    }

    public MissingSkillsFeedbackService missingSkillsFeedbackService() {
        return missingSkillsFeedbackService;
    }
}
