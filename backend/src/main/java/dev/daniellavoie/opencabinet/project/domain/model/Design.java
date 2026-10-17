package dev.daniellavoie.opencabinet.project.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The Room of a Project (Walls, their Openings and the Appliances against them), the Cabinets
 * placed in it and the Fillers between them.
 */
@Value
@Jacksonized
@Builder(toBuilder = true)
public class Design {
	List<Wall> walls;
	List<Cabinet> cabinets;
	List<Opening> openings;
	List<Appliance> appliances;
	List<Filler> fillers;
}
