package com.bioequivalence.engine

import com.bioequivalence.model.*
import jakarta.enterprise.context.ApplicationScoped
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.TDistribution
import kotlin.math.*

/**
 * Sample Size Engine — deterministic calculation of required study participants.
 *
 * Methods:
 *   - Standard crossover: Owen's exact method / Diletti-Hauschke-Steinijans
 *   - Replicate design: adjusted formula with RSABE scaling
 *   - Parallel: standard two-sample t-test
 *
 * References:
 *   - Chow SC, Liu JP (2009). Design and Analysis of Bioavailability/Bioequivalence Studies
 *   - EMA CPMP/EWP/QWP/1401/98 Rev.1
 *   - FDA Guidance for Industry: Bioequivalence Studies with Pharmacokinetic Endpoints (2003)
 */
@ApplicationScoped
class SampleSizeEngine {

    companion object {
        const val GMR_ASSUMED = 0.95       // assumed geometric mean ratio (typical for generics)
        const val THETA_LOWER = ln(0.80)   // ln(0.80) = -0.2231
        const val THETA_UPPER = ln(1.25)   // ln(1.25) =  0.2231
    }

    /**
     * Calculate required sample size based on study design.
     */
    fun calculate(
        cvIntra: Double,
        design: StudyDesign,
        power: Double = 0.80,
        alpha: Double = 0.05,
        dropoutRate: Double = 0.15,
        screenFailRate: Double = 0.20,
        gmr: Double = GMR_ASSUMED
    ): SampleSizeResult {
        val sigma = cvToSigma(cvIntra)
        val nPerSequence = when (design) {
            StudyDesign.TWO_WAY_CROSSOVER -> calcCrossoverN(sigma, power, alpha, gmr)
            StudyDesign.REPLICATE_DESIGN  -> calcReplicateN(sigma, power, alpha, gmr)
            StudyDesign.PARALLEL_DESIGN   -> calcParallelN(sigma, power, alpha, gmr)
            StudyDesign.THREE_WAY_CROSSOVER -> calcThreeWayN(sigma, power, alpha, gmr)
        }

        val sequences = when (design) {
            StudyDesign.TWO_WAY_CROSSOVER   -> 2
            StudyDesign.REPLICATE_DESIGN    -> 2
            StudyDesign.THREE_WAY_CROSSOVER -> 3
            StudyDesign.PARALLEL_DESIGN     -> 2
        }

        val nRandomized = nPerSequence * sequences
        val nAfterDropout = ceil(nRandomized / (1.0 - dropoutRate)).toInt()
        val nScreened = ceil(nAfterDropout / (1.0 - screenFailRate)).toInt()

        val method = when (design) {
            StudyDesign.TWO_WAY_CROSSOVER ->
                "Owen's exact method (2×2 crossover), CV=${"%.1f".format(cvIntra)}%, power=${"%.0f".format(power * 100)}%, α=$alpha, GMR=$gmr"
            StudyDesign.REPLICATE_DESIGN ->
                "Replicate design formula (RSABE), CV=${"%.1f".format(cvIntra)}%, power=${"%.0f".format(power * 100)}%, α=$alpha, GMR=$gmr"
            StudyDesign.PARALLEL_DESIGN ->
                "Two-sample t-test (parallel), CV=${"%.1f".format(cvIntra)}%, power=${"%.0f".format(power * 100)}%, α=$alpha, GMR=$gmr"
            StudyDesign.THREE_WAY_CROSSOVER ->
                "Three-way crossover formula, CV=${"%.1f".format(cvIntra)}%, power=${"%.0f".format(power * 100)}%, α=$alpha, GMR=$gmr"
        }

        return SampleSizeResult(
            nPerSequence = nPerSequence,
            nRandomized = nRandomized,
            nScreened = nScreened,
            design = design,
            power = power,
            alpha = alpha,
            gmrAssumed = gmr,
            calculation = method
        )
    }

    // ─── Design-specific calculations ─────────────────────────────────────────

    /**
     * 2×2 crossover sample size using iterative t-distribution method.
     * Formula: N per sequence = ceil((z_α + z_β)² × 2σ² / (ln(θ₁/θ₂)/2)²)
     * where θ₁=0.80, θ₂=1.25, σ = within-subject SD (log-scale)
     */
    private fun calcCrossoverN(sigma: Double, power: Double, alpha: Double, gmr: Double): Int {
        val delta = ln(gmr)
        val theta = THETA_UPPER // symmetric boundary

        return iterativeN(sigma, power, alpha, delta, theta, crossover = true)
    }

    /**
     * Replicate design (4-period, 2-sequence) — variance reduced by 2 measurements.
     * Within-subject variance in replicate = sigma²/2
     */
    private fun calcReplicateN(sigma: Double, power: Double, alpha: Double, gmr: Double): Int {
        val sigmaReplicate = sigma / sqrt(2.0)
        val delta = ln(gmr)
        val theta = THETA_UPPER

        return iterativeN(sigmaReplicate, power, alpha, delta, theta, crossover = true)
    }

    /**
     * Parallel design — two-sample t-test with between-subject variability.
     * Assumed σ_between ≈ 1.5 × σ_within
     */
    private fun calcParallelN(sigma: Double, power: Double, alpha: Double, gmr: Double): Int {
        val sigmaParallel = sigma * 1.5
        val delta = ln(gmr)
        val theta = THETA_UPPER

        return iterativeN(sigmaParallel, power, alpha, delta, theta, crossover = false)
    }

    /**
     * Three-way crossover — additional period increases precision.
     * Effective sigma reduced by sqrt(2/3)
     */
    private fun calcThreeWayN(sigma: Double, power: Double, alpha: Double, gmr: Double): Int {
        val sigmaThreeWay = sigma * sqrt(2.0 / 3.0)
        val delta = ln(gmr)
        val theta = THETA_UPPER

        return iterativeN(sigmaThreeWay, power, alpha, delta, theta, crossover = true)
    }

    /**
     * Iterative sample size search using t-distribution (Schuirmann's two one-sided tests).
     */
    private fun iterativeN(
        sigma: Double,
        power: Double,
        alpha: Double,
        delta: Double,   // ln(GMR)
        theta: Double,   // ln(1.25)
        crossover: Boolean
    ): Int {
        // Initial estimate using normal approximation
        val zAlpha = NormalDistribution().inverseCumulativeProbability(1.0 - alpha)
        val zBeta = NormalDistribution().inverseCumulativeProbability(power)

        val varFactor = if (crossover) 2.0 else 4.0  // crossover uses 2σ²; parallel uses 4σ²
        val nApprox = ceil(
            (zAlpha + zBeta).pow(2) * varFactor * sigma.pow(2) / (theta + delta).pow(2)
        ).toInt().coerceAtLeast(6)

        // Iterative refinement using t-distribution
        for (n in nApprox..(nApprox + 200)) {
            val df = if (crossover) 2 * (n - 1) else 2 * n - 2
            val tDist = TDistribution(df.toDouble())
            val tCrit = tDist.inverseCumulativeProbability(1.0 - alpha)

            val se = if (crossover) sqrt(2.0 * sigma.pow(2) / n)
                     else sqrt(4.0 * sigma.pow(2) / n)

            val ncp1 = (theta + delta) / se - tCrit
            val ncp2 = (theta - delta) / se - tCrit

            val achievedPower = tDist.cumulativeProbability(ncp1) +
                                tDist.cumulativeProbability(ncp2) - 1.0

            if (achievedPower >= power) return n
        }
        return nApprox + 200 // fallback
    }

    /** Convert CV% to log-scale within-subject SD */
    private fun cvToSigma(cv: Double): Double = sqrt(ln(1.0 + (cv / 100.0).pow(2)))
}
