package si.confreg.registration.domain;

import java.time.Instant;

/** A completed, stored registration (AC-001-05). */
public record Registration(
    String registrationNumber, Participant participant, Fees fees, Instant createdAt) {}
