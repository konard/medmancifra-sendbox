package com.bioequivalence.document

import com.bioequivalence.model.*
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Document Generator — creates structured synopsis in Markdown, JSON, and text formats.
 * All output is deterministic (no LLM involved here).
 */
@ApplicationScoped
class SynopsisGenerator {

    fun generate(
        drugName: String,
        design: StudyDesign,
        rsabe: RSABEResult,
        pkParams: PKParameters,
        sampleSize: SampleSizeResult,
        foodEffect: FoodEffect,
        regulatoryCompliance: List<RegulationType>
    ): StudySynopsis {
        val markdown = buildMarkdown(drugName, design, rsabe, pkParams, sampleSize, foodEffect, regulatoryCompliance)
        val synopsisText = buildSynopsisText(drugName, design, rsabe, pkParams, sampleSize, foodEffect)
        val jsonExport = buildJsonExport(drugName, design, rsabe, pkParams, sampleSize, foodEffect, regulatoryCompliance)

        return StudySynopsis(
            drugName = drugName,
            studyDesign = design,
            rsabe = rsabe,
            pkParameters = pkParams,
            sampleSize = sampleSize,
            foodEffect = foodEffect,
            regulatoryCompliance = regulatoryCompliance,
            synopsis = synopsisText,
            markdownExport = markdown,
            jsonExport = jsonExport
        )
    }

    private fun buildMarkdown(
        drugName: String,
        design: StudyDesign,
        rsabe: RSABEResult,
        pkParams: PKParameters,
        sampleSize: SampleSizeResult,
        foodEffect: FoodEffect,
        compliance: List<RegulationType>
    ): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val designRu = designToRu(design)
        val foodRu = foodEffectToRu(foodEffect)

        return """
# СИНОПСИС ИССЛЕДОВАНИЯ БИОЭКВИВАЛЕНТНОСТИ
## Препарат: $drugName
*Сгенерировано: $now | Версия регуляторных требований: ЕЭК №85 / EMA 2010 / FDA 2003*

---

## 1. Дизайн исследования
| Параметр | Значение |
|----------|----------|
| Дизайн | $designRu |
| CV_intra | ${"%.1f".format(rsabe.cvIntra)}% |
| t½ | ${"%.1f".format(pkParams.halfLife)} ч |
| Режим приёма | $foodRu |

## 2. RSABE-анализ
| Параметр | Значение |
|----------|----------|
| Решение | ${rsabe.decision.name} |
| Нижняя граница | ${"%.2f".format(rsabe.lowerBound)}% |
| Верхняя граница | ${"%.2f".format(rsabe.upperBound)}% |
| Масштабный коэффициент θ | ${rsabe.scalingFactor?.let { "%.4f".format(it) } ?: "не применяется"} |

**Обоснование:** ${rsabe.justification}

## 3. Фармакокинетические параметры
| Параметр | Значение |
|----------|----------|
| t½ | ${"%.1f".format(pkParams.halfLife)} ч |
| Washout (кратность t½) | ${"%.0f".format(pkParams.washoutPeriods)}× |
| Washout (часы) | ${"%.0f".format(pkParams.washoutHours)} ч |
| Washout (дни) | ${"%.1f".format(pkParams.washoutDays)} дней |
| Риск накопления | ${if (pkParams.accumulationRisk) "⚠️ ДА" else "Нет"} |

${pkParams.washoutWarning?.let { "\n> $it\n" } ?: ""}

## 4. Расчёт объёма выборки
| Параметр | Значение |
|----------|----------|
| N на последовательность | ${sampleSize.nPerSequence} |
| N рандомизации | **${sampleSize.nRandomized}** |
| N скрининга | **${sampleSize.nScreened}** |
| Статистическая мощность | ${"%.0f".format(sampleSize.power * 100)}% |
| Уровень значимости α | ${sampleSize.alpha} |
| Предполагаемый GMR | ${"%.2f".format(sampleSize.gmrAssumed)} |

*Метод расчёта: ${sampleSize.calculation}*

## 5. Регуляторное соответствие
${compliance.joinToString("\n") { "- ✅ ${regulationToDescription(it)}" }}

## 6. Следующие шаги
1. Подготовить протокол исследования согласно $designRu
2. ${if (foodEffect == FoodEffect.FASTING_AND_FED) "Запланировать 2 исследования: натощак + после еды" else "Исследование проводится натощак"}
3. ${if (rsabe.decision == RSABEDecision.APPLICABLE) "Включить в статанализ: RSABE (масштабированный критерий)" else "Применить стандартный критерий 80–125%"}
4. Обеспечить washout ≥ ${"%.0f".format(pkParams.washoutDays)} дней между периодами
5. Скринировать ≥ ${sampleSize.nScreened} субъектов для достижения N=${sampleSize.nRandomized}
        """.trimIndent()
    }

    private fun buildSynopsisText(
        drugName: String,
        design: StudyDesign,
        rsabe: RSABEResult,
        pkParams: PKParameters,
        sampleSize: SampleSizeResult,
        foodEffect: FoodEffect
    ): String {
        val designRu = designToRu(design)
        return "Исследование БЭ препарата $drugName: $designRu, " +
            "CV=${rsabe.cvIntra}%, t½=${pkParams.halfLife}ч, " +
            "RSABE: ${rsabe.decision}, " +
            "N рандомизации=${sampleSize.nRandomized}, N скрининга=${sampleSize.nScreened}, " +
            "режим: ${foodEffectToRu(foodEffect)}"
    }

    private fun buildJsonExport(
        drugName: String,
        design: StudyDesign,
        rsabe: RSABEResult,
        pkParams: PKParameters,
        sampleSize: SampleSizeResult,
        foodEffect: FoodEffect,
        compliance: List<RegulationType>
    ): Map<String, Any> = mapOf(
        "drugName" to drugName,
        "studyDesign" to design.name,
        "cvIntra" to rsabe.cvIntra,
        "rsabe" to mapOf(
            "decision" to rsabe.decision.name,
            "lowerBound" to rsabe.lowerBound,
            "upperBound" to rsabe.upperBound,
            "scalingFactor" to (rsabe.scalingFactor ?: "N/A")
        ),
        "pk" to mapOf(
            "halfLifeHours" to pkParams.halfLife,
            "washoutHours" to pkParams.washoutHours,
            "washoutDays" to pkParams.washoutDays,
            "accumulationRisk" to pkParams.accumulationRisk
        ),
        "sampleSize" to mapOf(
            "nPerSequence" to sampleSize.nPerSequence,
            "nRandomized" to sampleSize.nRandomized,
            "nScreened" to sampleSize.nScreened,
            "power" to sampleSize.power,
            "alpha" to sampleSize.alpha,
            "gmr" to sampleSize.gmrAssumed
        ),
        "foodEffect" to foodEffect.name,
        "regulatoryCompliance" to compliance.map { it.name },
        "generatedAt" to LocalDateTime.now().toString()
    )

    // ─── Localization helpers ─────────────────────────────────────────────────

    private fun designToRu(design: StudyDesign): String = when (design) {
        StudyDesign.TWO_WAY_CROSSOVER   -> "2×2 crossover (перекрёстный)"
        StudyDesign.REPLICATE_DESIGN    -> "Реплицированный дизайн (4-периодный)"
        StudyDesign.PARALLEL_DESIGN     -> "Параллельный дизайн"
        StudyDesign.THREE_WAY_CROSSOVER -> "3-way crossover"
    }

    private fun foodEffectToRu(fe: FoodEffect): String = when (fe) {
        FoodEffect.NONE            -> "Не оценивается"
        FoodEffect.FASTING_ONLY    -> "Натощак"
        FoodEffect.FASTING_AND_FED -> "Натощак + после еды (два исследования)"
    }

    private fun regulationToDescription(r: RegulationType): String = when (r) {
        RegulationType.EEK_85 -> "ЕЭК №85 (Решение Евразийской экономической комиссии)"
        RegulationType.EMA    -> "EMA CPMP/QWP/EWP/1401/98 Rev.1"
        RegulationType.FDA    -> "FDA Guidance for Industry: BE Studies with PK Endpoints (2003)"
    }
}
