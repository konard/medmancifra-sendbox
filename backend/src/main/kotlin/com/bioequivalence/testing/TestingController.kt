package com.bioequivalence.testing

import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * Testing Module Controller — exposes endpoints for:
 * - Full testing status (model info, test results, pipeline errors, DB mock)
 * - Individual sections (model, tests, pipeline, db)
 * - Trigger model training
 */
@Path("/api/v1/testing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Testing Module", description = "Model testing, metrics, training status and pipeline health")
class TestingController {

    @Inject
    lateinit var testingService: TestingService

    /**
     * Full testing status — all sections combined.
     */
    @GET
    @Path("/status")
    @Operation(
        summary = "Get Full Testing Status",
        description = "Returns model info, all test scenario results, pipeline errors, and DB mock readiness"
    )
    fun getStatus(): Response {
        val status = testingService.getTestingStatus()
        return Response.ok(status).build()
    }

    /**
     * Model info only — name, version, metrics, dataset links, training status.
     */
    @GET
    @Path("/model")
    @Operation(
        summary = "Get Model Info",
        description = "Returns model name, version, metrics, dataset reference, and training status"
    )
    fun getModelInfo(): Response {
        val status = testingService.getTestingStatus()
        return Response.ok(status.model).build()
    }

    /**
     * Test results only — all practical scenario runs.
     */
    @GET
    @Path("/results")
    @Operation(
        summary = "Get Test Results",
        description = "Run all practical medical scenario tests and return results"
    )
    fun getTestResults(): Response {
        val status = testingService.getTestingStatus()
        return Response.ok(mapOf(
            "results" to status.testResults,
            "summary" to status.summary
        )).build()
    }

    /**
     * Pipeline errors only.
     */
    @GET
    @Path("/pipeline")
    @Operation(
        summary = "Get Pipeline Errors",
        description = "Returns any errors detected during pipeline execution"
    )
    fun getPipelineErrors(): Response {
        val status = testingService.getTestingStatus()
        return Response.ok(mapOf(
            "errors" to status.pipelineErrors,
            "hasErrors" to status.pipelineErrors.isNotEmpty(),
            "criticalErrors" to status.pipelineErrors.count { it.isCritical }
        )).build()
    }

    /**
     * DB mock readiness check.
     */
    @GET
    @Path("/db")
    @Operation(
        summary = "Check DB Mock Readiness",
        description = "Check if the database mock is ready for read/write operations"
    )
    fun getDbStatus(): Response {
        val status = testingService.getTestingStatus()
        return Response.ok(status.dbMock).build()
    }

    /**
     * Trigger model training (async, returns job ID).
     */
    @POST
    @Path("/train")
    @Operation(
        summary = "Start Model Training",
        description = "Initiates model training. For the deterministic calculation engine, this reinitializes parameters."
    )
    fun startTraining(): Response {
        val result = testingService.startTraining()
        return Response.accepted(result).build()
    }
}
