package com.bupt.tarecruitment;

import com.bupt.tarecruitment.applicant.ApplicantCvIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantCvLibraryService;
import com.bupt.tarecruitment.applicant.ApplicantCvRepository;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewService;
import com.bupt.tarecruitment.applicant.ApplicantCvReviewSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantCvService;
import com.bupt.tarecruitment.applicant.ApplicantCvSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantProfileConsoleWorkflow;
import com.bupt.tarecruitment.applicant.ApplicantProfileIdGenerator;
import com.bupt.tarecruitment.applicant.ApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.ApplicantProfileService;
import com.bupt.tarecruitment.applicant.ApplicantProfileSwingDemo;
import com.bupt.tarecruitment.applicant.ApplicantProfileValidator;
import com.bupt.tarecruitment.applicant.TextFileApplicantCvRepository;
import com.bupt.tarecruitment.applicant.TextFileApplicantProfileRepository;
import com.bupt.tarecruitment.applicant.TextFileCvStorage;
import com.bupt.tarecruitment.application.ApplicationIdGenerator;
import com.bupt.tarecruitment.application.ApplicationRepository;
import com.bupt.tarecruitment.application.JobApplicationService;
import com.bupt.tarecruitment.application.TextFileApplicationRepository;
import com.bupt.tarecruitment.auth.AuthConsoleWorkflow;
import com.bupt.tarecruitment.auth.AuthService;
import com.bupt.tarecruitment.auth.AuthSwingDemo;
import com.bupt.tarecruitment.auth.AuthValidator;
import com.bupt.tarecruitment.auth.TextFileUserRepository;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRepository;
import com.bupt.tarecruitment.job.JobDetailApplyStubSwingDemo;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobPostingSwingDemo;
import com.bupt.tarecruitment.job.JobPostingService;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class ProjectModuleFactory {
    private final Path dataDirectory;
    private final UserRepository userRepository;
    private final AuthService authService;

    public ProjectModuleFactory(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
        this.userRepository = new TextFileUserRepository(dataDirectory);
        this.authService = new AuthService(userRepository, new AuthValidator());
    }

    public AuthConsoleWorkflow createUs00Workflow(InputStream input, PrintStream output) {
        return new AuthConsoleWorkflow(authService, input, output);
    }

    public AuthSwingDemo createAuthUi() {
        return new AuthSwingDemo(authService);
    }

    public AuthSwingDemo createAuthUi(Runnable onSessionChanged) {
        return new AuthSwingDemo(authService, onSessionChanged);
    }

    public Optional<UserAccount> getCurrentUser() {
        return authService.getCurrentUser();
    }

    public ApplicantProfileConsoleWorkflow createUs01Workflow(InputStream input, PrintStream output) {
        ApplicantProfileRepository repository = createProfileRepository();
        return new ApplicantProfileConsoleWorkflow(
            new ApplicantProfileService(repository, new ApplicantProfileValidator(), userRepository),
            new ApplicantProfileIdGenerator(repository),
            input,
            output
        );
    }

    public ApplicantProfileSwingDemo createUs01Ui() {
        ApplicantProfileRepository repository = createProfileRepository();
        return new ApplicantProfileSwingDemo(
            new ApplicantProfileService(repository, new ApplicantProfileValidator(), userRepository),
            new ApplicantProfileIdGenerator(repository)
        );
    }

    public ApplicantProfileSwingDemo createUs01Ui(String applicantUserId) {
        ApplicantProfileRepository repository = createProfileRepository();
        return new ApplicantProfileSwingDemo(
            new ApplicantProfileService(repository, new ApplicantProfileValidator(), userRepository),
            new ApplicantProfileIdGenerator(repository),
            applicantUserId,
            true
        );
    }

    public ApplicantCvSwingDemo createUs02Ui() {
        ApplicationRepository applicationRepository = createApplicationRepository();
        ApplicantProfileRepository profileRepository = createProfileRepository();
        ApplicantCvRepository cvRepository = createCvRepository();

        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(dataDirectory),
            userRepository
        );
        ApplicantCvService cvService = new ApplicantCvService(
            applicationRepository,
            cvRepository,
            new TextFileCvStorage(dataDirectory)
        );
        return new ApplicantCvSwingDemo(cvLibraryService, cvService);
    }

    public ApplicantCvSwingDemo createUs02Ui(String applicantUserId) {
        ApplicationRepository applicationRepository = createApplicationRepository();
        ApplicantProfileRepository profileRepository = createProfileRepository();
        ApplicantCvRepository cvRepository = createCvRepository();

        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(dataDirectory),
            userRepository
        );
        ApplicantCvService cvService = new ApplicantCvService(
            applicationRepository,
            cvRepository,
            new TextFileCvStorage(dataDirectory)
        );
        return new ApplicantCvSwingDemo(cvLibraryService, cvService, applicantUserId, true);
    }

    public ApplicantCvReviewSwingDemo createCvReviewUi() {
        return new ApplicantCvReviewSwingDemo(
            new ApplicantCvReviewService(
                createApplicationRepository(),
                createCvRepository(),
                createProfileRepository(),
                new TextFileCvStorage(dataDirectory)
            )
        );
    }

    public JobPostingSwingDemo createJobPostingUi() {
        JobRepository jobRepository = createJobRepository();
        return new JobPostingSwingDemo(jobRepository, new JobIdGenerator(jobRepository));
    }

    public JobPostingSwingDemo createJobPostingUi(String organiserId, String displayName) {
        JobRepository jobRepository = createJobRepository();
        return new JobPostingSwingDemo(jobRepository, new JobIdGenerator(jobRepository), organiserId, displayName);
    }

    public JobPostingService createJobPostingService() {
        JobRepository jobRepository = createJobRepository();
        return new JobPostingService(jobRepository, new JobIdGenerator(jobRepository), userRepository);
    }

    public JobDetailApplyStubSwingDemo createUs04Ui(String jobId, String applicantUserId) {
        JobRepository jobRepository = createJobRepository();
        ApplicationRepository applicationRepository = createApplicationRepository();
        ApplicantProfileRepository profileRepository = createProfileRepository();
        ApplicantCvRepository cvRepository = createCvRepository();

        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(dataDirectory),
            userRepository
        );
        JobApplicationService applicationService = new JobApplicationService(
            jobRepository,
            applicationRepository,
            new ApplicationIdGenerator(applicationRepository),
            profileRepository,
            cvRepository,
            userRepository
        );

        return new JobDetailApplyStubSwingDemo(jobId, applicantUserId, cvLibraryService, applicationService);
    }
    private ApplicantProfileRepository createProfileRepository() {
        return new TextFileApplicantProfileRepository(dataDirectory);
    }

    private ApplicantCvRepository createCvRepository() {
        return new TextFileApplicantCvRepository(dataDirectory);
    }

    private ApplicationRepository createApplicationRepository() {
        return new TextFileApplicationRepository(dataDirectory);
    }

    private JobRepository createJobRepository() {
        return new TextFileJobRepository(dataDirectory);
    }
}
