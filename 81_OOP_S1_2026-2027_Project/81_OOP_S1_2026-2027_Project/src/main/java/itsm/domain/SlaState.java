package itsm.domain;

/** Where an open incident stands against its SLA. AT_RISK is the early warning. */
public enum SlaState {

    ON_TRACK,
    AT_RISK,
    BREACHED
}
