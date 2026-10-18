package dev.daniellavoie.opencabinet.assistant.module.job;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import dev.daniellavoie.opencabinet.assistant.domain.port.backend.AssistantJobs;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * The assistant's requests, on virtual threads: each mostly waits for the model.
 */
@Slf4j
@Component
public class ExecutorAssistantJobs implements AssistantJobs {
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	@Override
	public void run(Runnable job) {
		executor.execute(() -> {
			try {
				job.run();
			} catch (RuntimeException exception) {
				log.error("Design assistant job failed", exception);
			}
		});
	}

	@PreDestroy
	void stop() {
		executor.shutdown();
	}
}
