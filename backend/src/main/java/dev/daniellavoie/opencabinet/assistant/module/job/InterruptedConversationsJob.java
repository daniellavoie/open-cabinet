package dev.daniellavoie.opencabinet.assistant.module.job;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import dev.daniellavoie.opencabinet.assistant.domain.port.api.ConversationDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Requests run in memory: at startup, those a restart cut short are closed, so their Conversations
 * take messages again.
 */
@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InterruptedConversationsJob implements ApplicationRunner {
	ConversationDomain domain;

	@Override
	public void run(ApplicationArguments args) {
		var interrupted = domain.interruptRunning();
		if (interrupted > 0) {
			log.info("Closed {} design assistant request(s) interrupted by a restart", interrupted);
		}
	}
}
