package com.bioequivalence.testing

import com.bioequivalence.model.StudyDesign
import java.time.LocalDateTime

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class ModelTrainingStatus {
    TRAINED,
    TRAINING_IN_PROGRESS,
    NOT_TRAINED,
    FAILED
}

enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    IN_PROGRESS
}

enum class DbMockStatus {
    READY,
    INITIALIZING,
    FAILED,
    NOT_INITIALIZED
}

// ─── Model Info DTOs ──────────────────────────────────────────────────────────

data class ModelMetrics(
    val accuracy: Double?,           // accuracy on validation set
    val rmse: Double?,               // root mean square error
    val mae: Double?,                // mean absolute error
    val r2Score: Double?,            // R² coefficient of determination
    val trainingDataPoints: Int?,    // number of training samples
    val validationDataPoints: Int?,  // number of validation samples
    val trainedAt: LocalDateTime?,
    val notes: String?
)

data class ModelInfo(
    val modelName: String,
    val modelVersion: String,
    val status: ModelTrainingStatus,
    val metrics: ModelMetrics?,
    val datasetName: String?,
    val datasetUrl: String?,
    val datasetDescription: String?,
    val canStartTraining: Boolean,
    val trainingMessage: String?
)

// ─── Test Result DTOs ─────────────────────────────────────────────────────────

data class TestScenarioResult(
    val scenarioId: String,
    val scenarioName: String,
    val description: String,
    val status: TestStatus,
    val expectedResult: String,
    val actualResult: String?,
    val errorMessage: String?,
    val executionTimeMs: Long,
    val executedAt: LocalDateTime
)

data class PipelineError(
    val stage: String,
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime,
    val isCritical: Boolean,
    val suggestion: String?
)

data class DbMockResult(
    val status: DbMockStatus,
    val writeTestPassed: Boolean,
    val readTestPassed: Boolean,
    val recordCount: Int,
    val connectionTimeMs: Long,
    val message: String
)

data class TestingSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val runAt: LocalDateTime
)

// ─── Main Testing Response ────────────────────────────────────────────────────

data class TestingStatusResponse(
    val model: ModelInfo,
    val testResults: List<TestScenarioResult>,
    val pipelineErrors: List<PipelineError>,
    val dbMock: DbMockResult,
    val summary: TestingSummary,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class TrainingStartResponse(
    val started: Boolean,
    val message: String,
    val estimatedDurationMinutes: Int?,
    val jobId: String?
)
