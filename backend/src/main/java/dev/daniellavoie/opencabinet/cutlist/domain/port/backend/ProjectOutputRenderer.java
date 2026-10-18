package dev.daniellavoie.opencabinet.cutlist.domain.port.backend;

import dev.daniellavoie.opencabinet.cutlist.domain.model.PrintedOutput;

public interface ProjectOutputRenderer {
	/**
	 * Prints a Project's Cut List, Nesting or Bill of Materials.
	 */
	byte[] pdf(PrintedOutput printed);
}
