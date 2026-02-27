package com.bioequivalence.engine

import com.bioequivalence.model.*
import jakarta.enterprise.context.ApplicationScoped
import kotlin.math.*

/**
 * Design Engine — deterministic core for study design selection.
 *
 * Decision logic:
 *   CV ≤ 30%  → 2×2 crossover
 *   CV > 30%  → check RSABE applicability
 *     RSABE OK  → Replicate design (4-period or 2×2×4)
 *     RSABE N/A → Parallel design
 *   t½ > 24h  → washout warning
 *   foodEffect → additional fed study
 */
@ApplicationScoped
class DesignEngine {

    companion object {
        const val CV_THRESHOLD_HVD = 30.0     // % — HVD threshold (EMA/EEK)
        const val CV_MAX_RSABE = 50.0          // % — practical max for RSABE
        const val WASHOUT_MULTIPLIER = 5.0     // standard 5× t½ washout
        const val HALF_LIFE_LONG = 24.0        // hours — threshold for washout warning
        const val HALF_LIFE_VERY_LONG = 72.0   // hours — very long t½
    }

    fun selectDesign(request: BECalculationRequest): StudyDesign {
        val rsabe = evaluateRSABE(request.cvIntra)
        return when {
            request.cvIntra <= CV_THRESHOLD_HVD -> StudyDesign.TWO_WAY_CROSSOVER
            rsabe.decision == RSABEDecision.APPLICABLE ||
            rsabe.decision == RSABEDecision.REQUIRED -> StudyDesign.REPLICATE_DESIGN
            else -> StudyDesign.PARALLEL_DESIGN
        }
    }

    fun evaluateRSABE(cvIntra: Double): RSABEResult {
        return when {
            cvIntra <= CV_THRESHOLD_HVD -> RSABEResult(
                cvIntra = cvIntra,
                decision = RSABEDecision.NOT_APPLICABLE,
                scalingFactor = null,
                lowerBound = 80.0,
                upperBound = 125.0,
                justification = "CV ≤ 30% — стандартные критерии 80–125%. RSABE не применяется (ЕЭК №85, п. 7.1)"
            )
            cvIntra in (CV_THRESHOLD_HVD + 0.001)..CV_MAX_RSABE -> {
                val sigma = cvToSigma(cvIntra)
                val scaling = computeScalingFactor(sigma)
                val (lo, hi) = scaledBoundaries(scaling)
                RSABEResult(
                    cvIntra = cvIntra,
                    decision = RSABEDecision.APPLICABLE,
                    scalingFactor = scaling,
                    lowerBound = lo,
                    upperBound = hi,
                    justification = "CV > 30% — высоковариабельный препарат (HVD). " +
                        "Применяется RSABE с масштабированием. log-граница = ${"%.4f".format(scaling)} (σ_R × √k). " +
                        "Индикативные границы при CV_intra=${cvIntra}%: ${"%.1f".format(lo)}%–${"%.1f".format(hi)}%. " +
                        "Основание: ЕЭК №85, EMA CPMP/QWP/EWP/1401/98 Rev.1 (динамический критерий в испытании)"
                )
            }
            else -> RSABEResult(
                cvIntra = cvIntra,
                decision = RSABEDecision.NOT_ALLOWED,
                scalingFactor = null,
                lowerBound = 80.0,
                upperBound = 125.0,
                justification = "CV > ${CV_MAX_RSABE}% — крайне высокая вариабельность. " +
                    "RSABE не применимо. Рекомендован параллельный дизайн с расширенным числом субъектов."
            )
        }
    }

    fun evaluatePKParameters(halfLife: Double): PKParameters {
        val washoutHours = halfLife * WASHOUT_MULTIPLIER
        val washoutDays = washoutHours / 24.0
        val accumulationRisk = halfLife > HALF_LIFE_LONG

        val warning = when {
            halfLife >= HALF_LIFE_VERY_LONG ->
                "⚠️ КРИТИЧНО: t½ = ${halfLife}ч (${halfLife / 24.0} дней). " +
                "Washout = ${washoutDays.roundToInt()} дней. " +
                "Для онкопрепаратов с длительным t½ рекомендуется параллельный дизайн."
            halfLife >= HALF_LIFE_LONG ->
                "⚠️ t½ = ${halfLife}ч. Washout = ${washoutDays.roundToInt()} дней. " +
                "Необходим тщательный контроль накопления (AUC0-∞/AUC0-t ≤ 1.20)."
            else -> null
        }

        return PKParameters(
            halfLife = halfLife,
            washoutPeriods = WASHOUT_MULTIPLIER,
            washoutHours = washoutHours,
            washoutDays = washoutDays,
            accumulationRisk = accumulationRisk,
            washoutWarning = warning
        )
    }

    fun determineFoodEffect(hasFoodEffect: Boolean): FoodEffect = when {
        !hasFoodEffect -> FoodEffect.FASTING_ONLY
        else -> FoodEffect.FASTING_AND_FED
    }

    // ─── Math helpers ─────────────────────────────────────────────────────────

    /** Convert CV% to within-subject standard deviation (log-scale) */
    private fun cvToSigma(cv: Double): Double = sqrt(ln(1 + (cv / 100.0).pow(2)))

    /**
     * Compute indicative RSABE scaling factor θ.
     * Per EMA CPMP/QWP/EWP/1401/98 Rev.1, the actual RSABE acceptance criterion
     * is dynamic: |ln(GMR)|² ≤ k × σ̂_R²  where k = (ln(1.25)/0.294)² ≈ 0.760
     * This is estimated per-trial. Here we compute the indicative boundaries
     * at the input CV_intra as the estimated σ_R.
     */
    private fun computeScalingFactor(sigma: Double): Double {
        // k = (ln(1.25) / σ_0)^2 where σ_0 = 0.294 (CV_ref = 30%)
        val k = 0.760
        // Return sqrt(k) * sigma as the log-scale upper bound
        return sqrt(k) * sigma
    }

    /**
     * Scaled acceptance boundaries per EMA dynamic RSABE criterion.
     * logBound = sqrt(k) × σ_R  →  boundaries = [e^(-logBound) × 100%, e^(logBound) × 100%]
     * Capped at 69.84–143.19% per EMA.
     */
    private fun scaledBoundaries(logBound: Double): Pair<Double, Double> {
        val upper = exp(logBound) * 100.0
        val lower = exp(-logBound) * 100.0
        return Pair(
            maxOf(lower, 69.84),
            minOf(upper, 143.19)
        )
    }
}
