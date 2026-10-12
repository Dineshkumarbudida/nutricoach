package org.nutricoach.targets.domain;

/**
 * The two adult equation sets published with the 2023 DRI EER method.
 *
 * <p>This input is requested only because the published equations use separate
 * coefficients. It must not be inferred from unrelated profile information.</p>
 */
public enum EnergyEquationSet {
    MALE,
    FEMALE
}
