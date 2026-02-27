package com.bioequivalence

import com.bioequivalence.engine.DesignEngine
import com.bioequivalence.model.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class DesignEngineTest {

    @Inject
    lateinit var designEngine: DesignEngine

    // ─── RSABE tests ──────────────────────────────────────────────────────────

    @Test
    fun `CV 20 percent should give standard crossover - not applicable RSABE`() {
        val result = designEngine.evaluateRSABE(20.0)
        assertEquals(RSABEDecision.NOT_APPLICABLE, result.decision)
        assertEquals(80.0, result.lowerBound, 0.01)
        assertEquals(125.0, result.upperBound, 0.01)
        assertNull(result.scalingFactor)
    }

    @Test
    fun `CV exactly 30 percent - borderline not applicable`() {
        val result = designEngine.evaluateRSABE(30.0)
        assertEquals(RSABEDecision.NOT_APPLICABLE, result.decision)
    }

    @Test
    fun `CV 42 percent - typical oncology drug - RSABE applicable`() {
        val result = designEngine.evaluateRSABE(42.0)
        assertEquals(RSABEDecision.APPLICABLE, result.decision)
        assertNotNull(result.scalingFactor)
        // Boundaries should be wider than 80-125%
        assertTrue(result.lowerBound < 80.0)
        assertTrue(result.upperBound > 125.0)
    }

    @Test
    fun `CV 60 percent - extreme - RSABE not allowed`() {
        val result = designEngine.evaluateRSABE(60.0)
        assertEquals(RSABEDecision.NOT_ALLOWED, result.decision)
    }

    // ─── Design selection tests ───────────────────────────────────────────────

    @Test
    fun `CV 20 percent should give TWO_WAY_CROSSOVER`() {
        val request = BECalculationRequest(
            drugName = "Imatinib", cvIntra = 20.0, halfLife = 18.0
        )
        val design = designEngine.selectDesign(request)
        assertEquals(StudyDesign.TWO_WAY_CROSSOVER, design)
    }

    @Test
    fun `CV 42 percent oncology drug should give REPLICATE_DESIGN`() {
        val request = BECalculationRequest(
            drugName = "Sorafenib", cvIntra = 42.0, halfLife = 48.0
        )
        val design = designEngine.selectDesign(request)
        assertEquals(StudyDesign.REPLICATE_DESIGN, design)
    }

    @Test
    fun `CV 55 percent extreme variability should give PARALLEL_DESIGN`() {
        val request = BECalculationRequest(
            drugName = "Extreme Drug", cvIntra = 55.0, halfLife = 12.0
        )
        val design = designEngine.selectDesign(request)
        assertEquals(StudyDesign.PARALLEL_DESIGN, design)
    }

    // ─── PK parameters tests ──────────────────────────────────────────────────

    @Test
    fun `short half life - no washout warning`() {
        val pk = designEngine.evaluatePKParameters(6.0)
        assertEquals(30.0, pk.washoutHours, 0.01)
        assertFalse(pk.accumulationRisk)
        assertNull(pk.washoutWarning)
    }

    @Test
    fun `t half life 48 hours - accumulation risk and warning`() {
        val pk = designEngine.evaluatePKParameters(48.0)
        assertTrue(pk.accumulationRisk)
        assertNotNull(pk.washoutWarning)
        assertEquals(240.0, pk.washoutHours, 0.01)
        assertEquals(10.0, pk.washoutDays, 0.01)
    }

    @Test
    fun `very long half life 96 hours - critical warning`() {
        val pk = designEngine.evaluatePKParameters(96.0)
        assertTrue(pk.accumulationRisk)
        assertNotNull(pk.washoutWarning)
        assertTrue(pk.washoutWarning!!.contains("КРИТИЧНО"))
    }

    // ─── Food effect tests ────────────────────────────────────────────────────

    @Test
    fun `no food effect - fasting only`() {
        val fe = designEngine.determineFoodEffect(false)
        assertEquals(FoodEffect.FASTING_ONLY, fe)
    }

    @Test
    fun `food effect present - fasting and fed`() {
        val fe = designEngine.determineFoodEffect(true)
        assertEquals(FoodEffect.FASTING_AND_FED, fe)
    }

    // ─── Full oncology use case (README example) ──────────────────────────────

    @Test
    fun `readme example - CV 42 percent - t half 48h - food effect`() {
        val request = BECalculationRequest(
            drugName = "TestOncoDrug",
            cvIntra = 42.0,
            halfLife = 48.0,
            foodEffect = true,
            power = 0.80,
            alpha = 0.05,
            dropoutRate = 0.15,
            screenFailRate = 0.20
        )
        val design = designEngine.selectDesign(request)
        val rsabe = designEngine.evaluateRSABE(request.cvIntra)
        val pk = designEngine.evaluatePKParameters(request.halfLife)
        val fe = designEngine.determineFoodEffect(request.foodEffect)

        assertEquals(StudyDesign.REPLICATE_DESIGN, design, "Should use replicate design for CV>30%")
        assertEquals(RSABEDecision.APPLICABLE, rsabe.decision, "RSABE should be applicable")
        assertTrue(pk.accumulationRisk, "Should flag accumulation risk for t½=48h")
        assertEquals(FoodEffect.FASTING_AND_FED, fe, "Should require both fasting and fed studies")
        assertEquals(240.0, pk.washoutHours, 0.01)
        assertEquals(10.0, pk.washoutDays, 0.01)
    }
}
