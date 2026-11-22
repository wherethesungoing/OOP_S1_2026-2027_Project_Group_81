package itsm.core;

import itsm.domain.Incident;

/** A possible duplicate and how similar it is (0.0 to 1.0). */
public record DuplicateMatch(Incident incident, double similarity) { }
