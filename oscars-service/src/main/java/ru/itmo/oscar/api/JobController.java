package ru.itmo.oscar.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.oscar.model.JobStatus;
import ru.itmo.oscar.service.InvalidParameterException;
import ru.itmo.oscar.service.JobService;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{job-id}/status")
    public JobStatus getJobStatus(@PathVariable("job-id") String value) {
        try {
            var jobId = UUID.fromString(value);
            if (!jobId.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }
            return jobService.get(jobId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidParameterException("job-id должен быть UUID");
        }
    }
}
