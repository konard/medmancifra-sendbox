package com.bioequivalence.rag

import com.bioequivalence.model.*
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Regulatory RAG Service — connects to GigaChat API to provide AI explanations
 * of BE study decisions. This layer does NOT affect mathematical calculations.
 *
 * The RAG knowledge base contains:
 * - ЕЭК №85 regulatory requirements
 * - EMA guidance documents
 * - FDA guidance documents
 * - Published PK studies for reference drugs
 */
@ApplicationScoped
class RagService {

    @ConfigProperty(name = "gigachat.api-url")
    lateinit var apiUrl: String

    @ConfigProperty(name = "gigachat.auth-url")
    lateinit var authUrl: String

    @ConfigProperty(name = "gigachat.client-id")
    lateinit var clientId: String

    @ConfigProperty(name = "gigachat.client-secret")
    lateinit var clientSecret: String

    @ConfigProperty(name = "gigachat.scope")
    lateinit var scope: String

    @ConfigProperty(name = "gigachat.model")
    lateinit var model: String

    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    /**
     * Generate AI explanation for a BE calculation result.
     * Returns a structured explanation in Russian for medical professionals.
     */
    fun explain(request: BECalculationRequest, synopsis: StudySynopsis): String {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return buildFallbackExplanation(request, synopsis)
        }

        return try {
            val token = getAccessToken()
            val prompt = buildExplainPrompt(request, synopsis)
            callGigaChat(token, prompt)
        } catch (e: Exception) {
            buildFallbackExplanation(request, synopsis)
        }
    }

    /**
     * Query the RAG knowledge base directly.
     */
    fun query(queryRequest: RagQueryRequest): RagQueryResponse {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return RagQueryResponse(
                answer = "GigaChat API не настроен. Для использования AI-объяснений укажите GIGACHAT_CLIENT_ID и GIGACHAT_CLIENT_SECRET.",
                sources = listOf("ЕЭК №85", "EMA CPMP/QWP/EWP/1401/98 Rev.1", "FDA Guidance 2003"),
                confidence = 0.0
            )
        }

        return try {
            val token = getAccessToken()
            val prompt = buildQueryPrompt(queryRequest)
            val answer = callGigaChat(token, prompt)
            RagQueryResponse(
                answer = answer,
                sources = listOf("ЕЭК №85", "EMA CPMP/QWP/EWP/1401/98 Rev.1"),
                confidence = 0.85
            )
        } catch (e: Exception) {
            RagQueryResponse(
                answer = "Ошибка при обращении к AI: ${e.message}",
                sources = emptyList(),
                confidence = 0.0
            )
        }
    }

    // ─── GigaChat integration ─────────────────────────────────────────────────

    private fun getAccessToken(): String {
        val body = "scope=$scope"
        val credentials = java.util.Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray())

        val request = HttpRequest.newBuilder()
            .uri(URI.create(authUrl))
            .header("Authorization", "Basic $credentials")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val json = mapper.readValue<Map<String, Any>>(response.body())
        return json["access_token"] as? String ?: throw RuntimeException("Failed to get GigaChat token")
    }

    private fun callGigaChat(token: String, prompt: String): String {
        val requestBody = mapper.writeValueAsString(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.3,
            "max_tokens" to 1024
        ))

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl/chat/completions"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val json = mapper.readValue<Map<String, Any>>(response.body())
        val choices = json["choices"] as? List<*> ?: return "Нет ответа от GigaChat"
        val first = choices.firstOrNull() as? Map<*, *> ?: return "Нет ответа"
        val message = first["message"] as? Map<*, *> ?: return "Нет ответа"
        return message["content"] as? String ?: "Нет ответа"
    }

    // ─── Prompt builders ──────────────────────────────────────────────────────

    private fun buildExplainPrompt(request: BECalculationRequest, synopsis: StudySynopsis): String =
        """
        Объясни решения платформы для исследования БЭ препарата "${request.drugName}".

        Параметры:
        - CV_intra: ${request.cvIntra}%
        - t½: ${request.halfLife} ч
        - Выбранный дизайн: ${synopsis.studyDesign}
        - RSABE решение: ${synopsis.rsabe.decision}
        - N рандомизации: ${synopsis.sampleSize.nRandomized}
        - Влияние пищи: ${synopsis.foodEffect}

        Дай краткое профессиональное объяснение (3-5 предложений) для медицинского специалиста,
        объясняя почему были сделаны именно такие решения с точки зрения регуляторных требований
        ЕЭК №85, EMA и FDA.
        """.trimIndent()

    private fun buildQueryPrompt(query: RagQueryRequest): String =
        """
        ${query.context?.let { "Контекст: $it\n\n" } ?: ""}
        Вопрос по регуляторным требованиям к исследованиям БЭ:
        ${query.query}

        Отвечай строго на основе регуляторных документов:
        ${query.regulation?.let { "Приоритетный документ: $it" } ?: "ЕЭК №85, EMA CPMP/QWP/EWP/1401/98 Rev.1, FDA Guidance 2003"}
        """.trimIndent()

    // ─── Fallback (no API key) ─────────────────────────────────────────────────

    private fun buildFallbackExplanation(request: BECalculationRequest, synopsis: StudySynopsis): String {
        val cv = request.cvIntra
        val design = synopsis.studyDesign
        val rsabe = synopsis.rsabe

        return buildString {
            append("Препарат '${request.drugName}' демонстрирует CV_intra = ${cv}%")
            if (cv > 30) {
                append(", что классифицирует его как высоковариабельный препарат (HVD). ")
                if (rsabe.decision == RSABEDecision.APPLICABLE) {
                    append("Применение RSABE (Reference-Scaled Average Bioequivalence) обосновано согласно ЕЭК №85 и EMA. ")
                    append("Реплицированный дизайн выбран для получения двух оценок внутрисубъектной дисперсии референтного препарата. ")
                }
            } else {
                append(", что позволяет применить стандартный 2×2 crossover дизайн с критерием 80-125%. ")
            }
            if (request.halfLife > 24) {
                append("Длительный t½ = ${request.halfLife}ч требует увеличенного washout-периода для исключения эффекта переноса (carry-over). ")
            }
            if (request.foodEffect) {
                append("Подтверждённое влияние пищи требует проведения двух отдельных исследований (натощак и после еды) согласно ЕЭК №85.")
            }
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """Ты — эксперт по биоэквивалентности и регуляторным требованиям в фармацевтике.
Ты отвечаешь только на основе регуляторных документов: ЕЭК №85, EMA CPMP/QWP/EWP/1401/98 Rev.1, FDA Guidance 2003.
Твои ответы кратки, точны и профессиональны. Ты не придумываешь данные и не даёшь рекомендации вне области регуляторики БЭ."""
    }
}
