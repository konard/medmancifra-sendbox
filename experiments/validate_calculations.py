#!/usr/bin/env python3
"""
Experiment: Validate BE Platform core calculations
Tests the Design Engine and Sample Size Engine logic in Python
to verify correctness before running the full Kotlin/Quarkus stack.

This serves as a reference implementation and proof-of-concept.
"""

import math
from dataclasses import dataclass
from typing import Optional
from scipy import stats
import numpy as np

# ─── Constants ────────────────────────────────────────────────────────────────
CV_THRESHOLD = 30.0
CV_MAX_RSABE = 50.0
WASHOUT_MULTIPLIER = 5.0
THETA_UPPER = math.log(1.25)   # 0.2231
THETA_LOWER = math.log(0.80)   # -0.2231
GMR_ASSUMED = 0.95
SIGMA_0 = 0.294  # reference SD for RSABE scaling (EMA)
K_SCALING = 0.760  # EMA regulatory constant

@dataclass
class RSABEResult:
    cv: float
    decision: str
    lower_bound: float
    upper_bound: float
    scaling_factor: Optional[float]

@dataclass
class SampleSizeResult:
    n_per_sequence: int
    n_randomized: int
    n_screened: int
    design: str


def cv_to_sigma(cv: float) -> float:
    """Convert CV% to log-scale within-subject SD."""
    return math.sqrt(math.log(1 + (cv / 100) ** 2))


def evaluate_rsabe(cv: float) -> RSABEResult:
    """RSABE applicability check (EMA/EEK_85)."""
    if cv <= CV_THRESHOLD:
        return RSABEResult(cv, "NOT_APPLICABLE", 80.0, 125.0, None)
    elif cv <= CV_MAX_RSABE:
        sigma = cv_to_sigma(cv)
        theta = math.exp(K_SCALING * SIGMA_0 ** 2)
        lower = max((1.0 / theta) * 100, 69.84)
        upper = min(theta * 100, 143.19)
        return RSABEResult(cv, "APPLICABLE", lower, upper, theta)
    else:
        return RSABEResult(cv, "NOT_ALLOWED", 80.0, 125.0, None)


def calc_crossover_n(sigma: float, power: float = 0.80, alpha: float = 0.05, gmr: float = GMR_ASSUMED) -> int:
    """Calculate n per sequence for 2×2 crossover (iterative t-distribution)."""
    delta = math.log(gmr)
    theta = THETA_UPPER

    z_alpha = stats.norm.ppf(1 - alpha)
    z_beta = stats.norm.ppf(power)
    n_approx = max(int(math.ceil((z_alpha + z_beta)**2 * 2 * sigma**2 / (theta + delta)**2)), 6)

    for n in range(n_approx, n_approx + 200):
        df = 2 * (n - 1)
        t_crit = stats.t.ppf(1 - alpha, df)
        se = math.sqrt(2 * sigma**2 / n)
        ncp1 = (theta + delta) / se - t_crit
        ncp2 = (theta - delta) / se - t_crit
        achieved_power = stats.t.cdf(ncp1, df) + stats.t.cdf(ncp2, df) - 1.0
        if achieved_power >= power:
            return n
    return n_approx + 200


def calc_sample_size(cv: float, design: str, power: float = 0.80,
                     alpha: float = 0.05, dropout: float = 0.15, screen_fail: float = 0.20,
                     gmr: float = GMR_ASSUMED) -> SampleSizeResult:
    """Full sample size calculation with dropout/screen-fail correction."""
    sigma = cv_to_sigma(cv)

    if design == "TWO_WAY_CROSSOVER":
        n_seq = calc_crossover_n(sigma, power, alpha, gmr)
        sequences = 2
    elif design == "REPLICATE_DESIGN":
        n_seq = calc_crossover_n(sigma / math.sqrt(2), power, alpha, gmr)
        sequences = 2
    elif design == "PARALLEL_DESIGN":
        n_seq = calc_crossover_n(sigma * 1.5, power, alpha, gmr)
        sequences = 2
    else:
        n_seq = calc_crossover_n(sigma * math.sqrt(2/3), power, alpha, gmr)
        sequences = 3

    n_rand = n_seq * sequences
    n_dropout = math.ceil(n_rand / (1 - dropout))
    n_screened = math.ceil(n_dropout / (1 - screen_fail))

    return SampleSizeResult(n_seq, n_rand, int(n_screened), design)


# ─── Test Cases ───────────────────────────────────────────────────────────────

def run_tests():
    print("=" * 70)
    print("BE Platform — Core Calculations Validation")
    print("=" * 70)

    # Test 1: Standard crossover CV=20%
    print("\n📋 Test 1: Imatinib (CV=20%, standard crossover)")
    rsabe = evaluate_rsabe(20.0)
    ss = calc_sample_size(20.0, "TWO_WAY_CROSSOVER")
    print(f"  RSABE: {rsabe.decision} (expected: NOT_APPLICABLE) {'✅' if rsabe.decision == 'NOT_APPLICABLE' else '❌'}")
    print(f"  Bounds: {rsabe.lower_bound:.1f}–{rsabe.upper_bound:.1f}% (expected: 80–125%) {'✅' if rsabe.lower_bound == 80.0 else '❌'}")
    print(f"  N randomized: {ss.n_randomized} (expected: 10-30)")
    print(f"  N screened: {ss.n_screened}")

    # Test 2: README example (CV=42%, RSABE)
    print("\n📋 Test 2: Sorafenib — README example (CV=42%, t½=48h, food effect)")
    rsabe = evaluate_rsabe(42.0)
    ss = calc_sample_size(42.0, "REPLICATE_DESIGN", dropout=0.15, screen_fail=0.20)
    print(f"  RSABE: {rsabe.decision} (expected: APPLICABLE) {'✅' if rsabe.decision == 'APPLICABLE' else '❌'}")
    print(f"  θ scaling: {rsabe.scaling_factor:.4f}")
    print(f"  Bounds: {rsabe.lower_bound:.2f}–{rsabe.upper_bound:.2f}%")
    print(f"  N randomized: {ss.n_randomized} (README example: ~64) {'✅' if 40 <= ss.n_randomized <= 80 else '❌'}")
    print(f"  N screened: {ss.n_screened} (README example: ~92)")

    # Test 3: Extreme CV → NOT_ALLOWED
    print("\n📋 Test 3: Extreme CV=60% → NOT_ALLOWED")
    rsabe = evaluate_rsabe(60.0)
    ss = calc_sample_size(60.0, "PARALLEL_DESIGN")
    print(f"  RSABE: {rsabe.decision} (expected: NOT_ALLOWED) {'✅' if rsabe.decision == 'NOT_ALLOWED' else '❌'}")
    print(f"  Parallel N randomized: {ss.n_randomized}")

    # Test 4: Power comparison
    print("\n📋 Test 4: Power 80% vs 90% (CV=25%)")
    ss80 = calc_sample_size(25.0, "TWO_WAY_CROSSOVER", power=0.80)
    ss90 = calc_sample_size(25.0, "TWO_WAY_CROSSOVER", power=0.90)
    print(f"  N (80%): {ss80.n_randomized}, N (90%): {ss90.n_randomized}")
    print(f"  90% needs more subjects: {'✅' if ss90.n_randomized > ss80.n_randomized else '❌'}")

    # Test 5: Washout calculation
    print("\n📋 Test 5: Washout periods")
    for t_half in [6, 18, 48, 99]:
        washout_hours = t_half * WASHOUT_MULTIPLIER
        washout_days = washout_hours / 24
        warning = "⚠️ КРИТИЧНО" if t_half >= 72 else ("⚠️" if t_half >= 24 else "")
        print(f"  t½={t_half}h → washout={washout_hours:.0f}h ({washout_days:.1f}d) {warning}")

    # Summary table
    print("\n" + "=" * 70)
    print("📊 Summary — Sample Sizes for Common Oncology Drugs")
    print("=" * 70)
    print(f"{'Drug':<20} {'CV%':<8} {'Design':<20} {'N_rand':<10} {'N_screen':<10}")
    print("-" * 70)

    drugs = [
        ("Imatinib", 20.1, "TWO_WAY_CROSSOVER"),
        ("Sorafenib", 42.3, "REPLICATE_DESIGN"),
        ("Sunitinib", 38.7, "REPLICATE_DESIGN"),
        ("Erlotinib", 26.4, "TWO_WAY_CROSSOVER"),
        ("Cabozantinib", 41.5, "REPLICATE_DESIGN"),
        ("Lapatinib", 45.0, "REPLICATE_DESIGN"),
    ]

    for drug, cv, design in drugs:
        ss = calc_sample_size(cv, design)
        print(f"  {drug:<18} {cv:<8} {design:<20} {ss.n_randomized:<10} {ss.n_screened:<10}")

    print("\n✅ All validation tests complete")


if __name__ == "__main__":
    run_tests()
