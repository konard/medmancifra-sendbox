package com.bioequivalence.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDateTime

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class StudyDesign {
    TWO_WAY_CROSSOVER,
    REPLICATE_DESIGN,
    PARALLEL_DESIGN,
    THREE_WAY_CROSSOVER
}

enum class FoodEffect {
    NONE,
    FASTING_ONLY,
    FASTING_AND_FED
}

enum class RegulationType {
    EEK_85,
    EMA,
    FDA
}

enum class RSABEDecision {
    NOT_APPLICABLE,
    APPLICABLE,
    REQUIRED,
    NOT_ALLOWED
}

// ─── Request DTOs ─────────────────────────────────────────────────────────────

data class BECalculationRequest(
    @field:NotBlank
    val drugName: String,

    @field:DecimalMin("0.0") @field:DecimalMax("200.0")
    val cvIntra: Double,                // % intra-subject coefficient of variation

    @field:DecimalMin("0.0")
    val halfLife: Double,               // hours

    @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val power: Double = 0.80,           // statistical power (default 80%)

    @field:DecimalMin("0.0") @field:DecimalMax("0.20")
    val alpha: Double = 0.05,           // significance level (default 5%)

    @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val dropoutRate: Double = 0.15,     // expected dropout rate (default 15%)

    @field:DecimalMin("0.0") @field:DecimalMax("1.0")
    val screenFailRate: Double = 0.20,  // screen-fail rate (default 20%)

    val foodEffect: Boolean = false,

    val forceRSABE: Boolean? = null,    // null = auto-decide

    val notes: String? = null
)

// ─── Response DTOs ────────────────────────────────────────────────────────────

data class PKParameters(
    val halfLife: Double,
    val washoutPeriods: Double,     // recommended washout in half-lives
    val washoutHours: Double,       // recommended washout in hours
    val washoutDays: Double,        // recommended washout in days
    val accumulationRisk: Boolean,  // true if t½ > 24h
    val washoutWarning: String?
)

data class RSABEResult(
    val cvIntra: Double,
    val decision: RSABEDecision,
    val scalingFactor: Double?,     // θ scaling factor when RSABE applicable
    val lowerBound: Double,         // lower acceptance boundary
    val upperBound: Double,         // upper acceptance boundary
    val justification: String
)

data class SampleSizeResult(
    val nPerSequence: Int,          // subjects per sequence group
    val nRandomized: Int,           // total randomized
    val nScreened: Int,             // total to screen
    val design: StudyDesign,
    val power: Double,
    val alpha: Double,
    val gmrAssumed: Double,         // assumed GMR (usually 0.95)
    val calculation: String         // formula/method used
)

data class StudySynopsis(
    val drugName: String,
    val studyDesign: StudyDesign,
    val rsabe: RSABEResult,
    val pkParameters: PKParameters,
    val sampleSize: SampleSizeResult,
    val foodEffect: FoodEffect,
    val regulatoryCompliance: List<RegulationType>,
    val synopsis: String,           // full auto-generated synopsis text
    val markdownExport: String,
    val jsonExport: Map<String, Any>
)

data class BECalculationResponse(
    val requestId: String,
    val drugName: String,
    val studyDesign: StudyDesign,
    val rsabe: RSABEResult,
    val pkParameters: PKParameters,
    val sampleSize: SampleSizeResult,
    val foodEffect: FoodEffect,
    val synopsis: StudySynopsis,
    val aiExplanation: String?,
    val calculatedAt: LocalDateTime = LocalDateTime.now(),
    val regulatoryVersion: String = "ЕЭК №85 / EMA 2010 / FDA 2003"
)

// ─── RAG / AI ─────────────────────────────────────────────────────────────────

data class RagQueryRequest(
    @field:NotBlank
    val query: String,
    val context: String? = null,
    val regulation: RegulationType? = null
)

data class RagQueryResponse(
    val answer: String,
    val sources: List<String>,
    val confidence: Double
)

// ─── Database Entities ────────────────────────────────────────────────────────

@Entity
@Table(name = "be_calculations")
class BECalculationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(nullable = false)
    var drugName: String = ""

    @Column(nullable = false)
    var cvIntra: Double = 0.0

    @Column(nullable = false)
    var halfLife: Double = 0.0

    @Column(nullable = false)
    var studyDesign: String = ""

    @Column(nullable = false)
    var nRandomized: Int = 0

    @Column(nullable = false)
    var nScreened: Int = 0

    @Column(nullable = false)
    var rsabeApplied: Boolean = false

    @Column(nullable = false)
    var foodEffect: String = ""

    @Column(columnDefinition = "TEXT")
    var synopsisMarkdown: String = ""

    @Column(columnDefinition = "TEXT")
    var inputJson: String = ""

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
}

@Entity
@Table(name = "pk_reference_data")
class PKReferenceDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null

    @Column(nullable = false)
    var drugName: String = ""

    @Column(nullable = false)
    var cvIntra: Double = 0.0

    @Column
    var halfLife: Double? = null

    @Column
    var tmax: Double? = null

    @Column
    var bioavailability: Double? = null

    @Column
    var source: String = ""

    @Column
    var pubmedId: String? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
}
