package com.bioequivalence.api

import com.bioequivalence.engine.BECalculationService
import com.bioequivalence.model.*
import com.bioequivalence.rag.RagService
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bioequivalence API", description = "AI Platform for BE Research Automation")
class BEController {

    @Inject
    lateinit var calculationService: BECalculationService

    @Inject
    lateinit var ragService: RagService

    /**
     * Main calculation endpoint — full BE study design in one call.
     */
    @POST
    @Path("/calculate")
    @Operation(
        summary = "Calculate BE Study Design",
        description = "Deterministic calculation of study design, RSABE, PK parameters, sample size, and synopsis generation"
    )
    @APIResponse(
        responseCode = "200",
        description = "Calculation result with synopsis",
        content = [Content(schema = Schema(implementation = BECalculationResponse::class))]
    )
    fun calculate(@Valid @RequestBody request: BECalculationRequest): Response {
        val result = calculationService.calculate(request)
        return Response.ok(result).build()
    }

    /**
     * Design-only endpoint for quick design selection.
     */
    @POST
    @Path("/design")
    @Operation(
        summary = "Select Study Design",
        description = "Quick design selection based on CV and t½"
    )
    fun selectDesign(@Valid @RequestBody request: BECalculationRequest): Response {
        val design = calculationService.calculate(request).studyDesign
        return Response.ok(mapOf(
            "drugName" to request.drugName,
            "cvIntra" to request.cvIntra,
            "design" to design.name,
            "designDescription" to designDescription(design)
        )).build()
    }

    /**
     * RSABE evaluation endpoint.
     */
    @POST
    @Path("/rsabe")
    @Operation(
        summary = "Evaluate RSABE Applicability",
        description = "Check if Reference-Scaled Average Bioequivalence is applicable"
    )
    fun evaluateRSABE(body: Map<String, Double>): Response {
        val cv = body["cvIntra"] ?: return Response.status(400)
            .entity(mapOf("error" to "cvIntra is required")).build()
        val result = calculationService.calculate(
            BECalculationRequest(drugName = "query", cvIntra = cv, halfLife = 1.0)
        ).rsabe
        return Response.ok(result).build()
    }

    /**
     * Sample size calculation endpoint.
     */
    @POST
    @Path("/sample-size")
    @Operation(
        summary = "Calculate Sample Size",
        description = "Calculate required number of subjects (N randomized + N screened)"
    )
    fun calculateSampleSize(@Valid @RequestBody request: BECalculationRequest): Response {
        val result = calculationService.calculate(request)
        return Response.ok(result.sampleSize).build()
    }

    /**
     * RAG query endpoint for regulatory questions.
     */
    @POST
    @Path("/rag/query")
    @Operation(
        summary = "Query Regulatory Knowledge Base",
        description = "Ask regulatory questions answered by GigaChat RAG (ЕЭК №85, EMA, FDA)"
    )
    fun ragQuery(@Valid @RequestBody request: RagQueryRequest): Response {
        val result = ragService.query(request)
        return Response.ok(result).build()
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    @Operation(summary = "Health Check", description = "Check API availability")
    fun health(): Response = Response.ok(mapOf(
        "status" to "UP",
        "service" to "BE Platform API v1.0.0",
        "description" to "AI Platform for Bioequivalence Research Automation (Oncology)"
    )).build()

    /**
     * List supported study designs with descriptions.
     */
    @GET
    @Path("/designs")
    @Operation(summary = "List Study Designs", description = "All supported study designs with decision criteria")
    fun listDesigns(): Response = Response.ok(listOf(
        mapOf(
            "design" to "TWO_WAY_CROSSOVER",
            "nameRu" to "2×2 Crossover",
            "criteria" to "CV ≤ 30%",
            "boundaries" to "80–125%",
            "description" to "Стандартный перекрёстный дизайн. 2 периода, 2 последовательности."
        ),
        mapOf(
            "design" to "REPLICATE_DESIGN",
            "nameRu" to "Реплицированный дизайн",
            "criteria" to "CV > 30% + RSABE применимо",
            "boundaries" to "Масштабированные (69.84–143.19%)",
            "description" to "4-периодный дизайн. Получение двух оценок σ_R для RSABE."
        ),
        mapOf(
            "design" to "PARALLEL_DESIGN",
            "nameRu" to "Параллельный дизайн",
            "criteria" to "CV > 50% или длительный t½",
            "boundaries" to "80–125%",
            "description" to "Параллельные группы без перекреста. Большая выборка, но нет эффекта переноса."
        ),
        mapOf(
            "design" to "THREE_WAY_CROSSOVER",
            "nameRu" to "3-way Crossover",
            "criteria" to "3 формулы для сравнения",
            "boundaries" to "80–125%",
            "description" to "3 периода, 3 последовательности. Для сравнения нескольких формуляций."
        )
    )).build()

    private fun designDescription(design: StudyDesign): String = when (design) {
        StudyDesign.TWO_WAY_CROSSOVER   -> "2×2 Crossover — стандартный перекрёстный дизайн (CV ≤ 30%)"
        StudyDesign.REPLICATE_DESIGN    -> "Реплицированный дизайн с RSABE (CV > 30%)"
        StudyDesign.PARALLEL_DESIGN     -> "Параллельный дизайн (CV > 50% или длительный t½)"
        StudyDesign.THREE_WAY_CROSSOVER -> "3-way Crossover (3 формуляции)"
    }
}
