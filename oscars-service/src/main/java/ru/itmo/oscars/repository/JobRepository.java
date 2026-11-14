package ru.itmo.oscars.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {
}
