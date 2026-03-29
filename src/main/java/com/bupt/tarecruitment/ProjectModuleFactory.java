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
import com.bupt.tarecruitment.job.JobDetailApplyStubSwingDemo;
import com.bupt.tarecruitment.job.JobIdGenerator;
import com.bupt.tarecruitment.job.JobPostingSwingDemo;
import com.bupt.tarecruitment.job.JobRepository;
import com.bupt.tarecruitment.job.TextFileJobRepository;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

public final class ProjectModuleFactory {
    private final Path dataDirectory;

    public ProjectModuleFactory(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
    }

    public AuthConsoleWorkflow createUs00Workflow(InputStream input, PrintStream output) {
        return new AuthConsoleWorkflow(createAuthService(), input, output);
    }

    public AuthSwingDemo createAuthUi() {
        return new AuthSwingDemo(createAuthService());
    }

    public ApplicantProfileConsoleWorkflow createUs01Workflow(InputStream input, PrintStream output) {
        ApplicantProfileRepository repository = createProfileRepository();
        return new ApplicantProfileConsoleWorkflow(
            new ApplicantProfileService(repository, new ApplicantProfileValidator()),
            new ApplicantProfileIdGenerator(repository),
            input,
            output
        );
    }

    public ApplicantProfileSwingDemo createUs01Ui() {
        ApplicantProfileRepository repository = createProfileRepository();
        return new ApplicantProfileSwingDemo(
            new ApplicantProfileService(repository, new ApplicantProfileValidator()),
            new ApplicantProfileIdGenerator(repository)
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
            new TextFileCvStorage(dataDirectory)
        );
        ApplicantCvService cvService = new ApplicantCvService(
            applicationRepository,
            cvRepository,
            new TextFileCvStorage(dataDirectory)
        );
        return new ApplicantCvSwingDemo(cvLibraryService, cvService);
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

    public JobDetailApplyStubSwingDemo createUs04Ui(String jobId, String applicantUserId) {
        JobRepository jobRepository = createJobRepository();
        ApplicationRepository applicationRepository = createApplicationRepository();
        ApplicantProfileRepository profileRepository = createProfileRepository();
        ApplicantCvRepository cvRepository = createCvRepository();

        ApplicantCvLibraryService cvLibraryService = new ApplicantCvLibraryService(
            profileRepository,
            cvRepository,
            new ApplicantCvIdGenerator(cvRepository),
            new TextFileCvStorage(dataDirectory)
        );
        JobApplicationService applicationService = new JobApplicationService(
            jobRepository,
            applicationRepository,
            new ApplicationIdGenerator(applicationRepository),
            profileRepository,
            cvRepository
        );

        return new JobDetailApplyStubSwingDemo(jobId, applicantUserId, cvLibraryService, applicationService);
    }

    private AuthService createAuthService() {
        return new AuthService(new TextFileUserRepository(dataDirectory), new AuthValidator());
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
