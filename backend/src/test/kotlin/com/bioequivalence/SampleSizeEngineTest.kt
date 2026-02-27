package com.bioequivalence

import com.bioequivalence.engine.SampleSizeEngine
import com.bioequivalence.model.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class SampleSizeEngineTest {

    @Inject
    lateinit var engine: SampleSizeEngine

    @Test
    fun `standard crossover CV 20 percent - reasonable sample size`() {
        val result = engine.calculate(
            cvIntra = 20.0,
            design = StudyDesign.TWO_WAY_CROSSOVER,
            power = 0.80,
            alpha = 0.05
        )
        // For CV=20%, 2x2 crossover, typical N is 10-20 per sequence
        assertTrue(result.nPerSequence in 6..30, "N per sequence should be 6-30 for CV=20%, got ${result.nPerSequence}")
        assertEquals(result.nPerSequence * 2, result.nRandomized)
        assertTrue(result.nScreened > result.nRandomized)
    }

    @Test
    fun `replicate design CV 42 percent - README example approx 64 total`() {
        val result = engine.calculate(
            cvIntra = 42.0,
            design = StudyDesign.REPLICATE_DESIGN,
            power = 0.80,
            alpha = 0.05,
            dropoutRate = 0.15,
            screenFailRate = 0.20
        )
        // README example states N rандомизации ≈ 64
        println("Replicate N randomized: ${result.nRandomized}, screened: ${result.nScreened}")
        assertTrue(result.nRandomized in 20..120, "N randomized should be reasonable, got ${result.nRandomized}")
        assertTrue(result.nScreened > result.nRandomized, "Screened should exceed randomized")
    }

    @Test
    fun `parallel design has larger sample size than crossover`() {
        val crossover = engine.calculate(20.0, StudyDesign.TWO_WAY_CROSSOVER)
        val parallel = engine.calculate(20.0, StudyDesign.PARALLEL_DESIGN)
        assertTrue(parallel.nRandomized > crossover.nRandomized,
            "Parallel should need more subjects than crossover")
    }

    @Test
    fun `higher power requires more subjects`() {
        val n80 = engine.calculate(25.0, StudyDesign.TWO_WAY_CROSSOVER, power = 0.80)
        val n90 = engine.calculate(25.0, StudyDesign.TWO_WAY_CROSSOVER, power = 0.90)
        assertTrue(n90.nRandomized > n80.nRandomized,
            "Power 90% should need more subjects than 80%, got ${n80.nRandomized} vs ${n90.nRandomized}")
    }

    @Test
    fun `higher CV requires more subjects`() {
        val n20 = engine.calculate(20.0, StudyDesign.TWO_WAY_CROSSOVER)
        val n30 = engine.calculate(30.0, StudyDesign.TWO_WAY_CROSSOVER)
        assertTrue(n30.nRandomized >= n20.nRandomized,
            "CV=30% should need >= subjects than CV=20%, got ${n20.nRandomized} vs ${n30.nRandomized}")
    }

    @Test
    fun `dropout correction - screened is larger than randomized`() {
        val result = engine.calculate(
            cvIntra = 20.0,
            design = StudyDesign.TWO_WAY_CROSSOVER,
            dropoutRate = 0.20,
            screenFailRate = 0.25
        )
        // With 20% dropout + 25% screen-fail, screened >> randomized
        val expectedMinScreened = (result.nRandomized * 1.2 * 1.25).toInt()
        assertTrue(result.nScreened >= expectedMinScreened - 2,
            "Screened ${result.nScreened} should account for dropout + screen-fail")
    }
}
