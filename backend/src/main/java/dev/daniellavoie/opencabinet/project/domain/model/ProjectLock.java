package dev.daniellavoie.opencabinet.project.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Who is editing a Project. Only the holder can save it; the lock lapses unless renewed.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class ProjectLock {
	UUID holderId;
	String holderName;
	LocalDateTime expirationTimestamp;
	/**
	 * Whether the Member asking holds the lock.
	 */
	boolean mine;
}
