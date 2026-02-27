package com.bioequivalence.engine

import com.bioequivalence.document.SynopsisGenerator
import com.bioequivalence.model.*
import com.bioequivalence.rag.RagService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.UUID

/**
 * Main orchestration service — coordinates Design Engine, Sample Size Engine,
 * and document generation for a complete BE study plan.
 */
@ApplicationScoped
class BECalculationService {

    @Inject
    lateinit var designEngine: DesignEngine

    @Inject
    lateinit var sampleSizeEngine: SampleSizeEngine

    @Inject
    lateinit var synopsisGenerator: SynopsisGenerator

    @Inject
    lateinit var ragService: RagService

    fun calculate(request: BECalculationRequest): BECalculationResponse {
        // 1. Evaluate RSABE
        val rsabe = designEngine.evaluateRSABE(request.cvIntra)

        // 2. Select study design
        val design = if (request.forceRSABE != null) {
            if (request.forceRSABE) StudyDesign.REPLICATE_DESIGN else StudyDesign.TWO_WAY_CROSSOVER
        } else {
            designEngine.selectDesign(request)
        }

        // 3. PK parameters (washout etc.)
        val pkParams = designEngine.evaluatePKParameters(request.halfLife)

        // 4. Sample size calculation
        val sampleSize = sampleSizeEngine.calculate(
            cvIntra = request.cvIntra,
            design = design,
            power = request.power,
            alpha = request.alpha,
            dropoutRate = request.dropoutRate,
            screenFailRate = request.screenFailRate
        )

        // 5. Food effect
        val foodEffect = designEngine.determineFoodEffect(request.foodEffect)

        // 6. Regulatory compliance list
        val regulatoryCompliance = listOf(RegulationType.EEK_85, RegulationType.EMA, RegulationType.FDA)

        // 7. Generate synopsis
        val synopsis = synopsisGenerator.generate(
            drugName = request.drugName,
            design = design,
            rsabe = rsabe,
            pkParams = pkParams,
            sampleSize = sampleSize,
            foodEffect = foodEffect,
            regulatoryCompliance = regulatoryCompliance
        )

        // 8. Optional AI explanation (non-deterministic layer, does not affect math)
        val aiExplanation = try {
            ragService.explain(request, synopsis)
        } catch (e: Exception) {
            "AI-объяснение временно недоступно: ${e.message}"
        }

        return BECalculationResponse(
            requestId = UUID.randomUUID().toString(),
            drugName = request.drugName,
            studyDesign = design,
            rsabe = rsabe,
            pkParameters = pkParams,
            sampleSize = sampleSize,
            foodEffect = foodEffect,
            synopsis = synopsis,
            aiExplanation = aiExplanation
        )
    }
}
