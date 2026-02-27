# 🚀 AI-ПЛАТФОРМА АВТОМАТИЗАЦИИ ИССЛЕДОВАНИЙ БИОЭКВИВАЛЕНТНОСТИ (ОНКОЛОГИЯ)

## Реализованное решение

### Архитектура

```
┌─────────────────────────────────────┐
│  FRONTEND (React + TypeScript)      │
│  • Дизайн-калькулятор               │
│  • Панель результатов               │
│  • AI-чат (GigaChat RAG)            │
│  • Экспорт синопсиса (MD/JSON)      │
└────────────────┬────────────────────┘
                 │ REST / HTTPS
                 ▼
┌─────────────────────────────────────┐
│  BACKEND (Kotlin + Quarkus 3.8)     │
│                                     │
│  POST /api/v1/calculate  ← главный  │
│  POST /api/v1/design                │
│  POST /api/v1/rsabe                 │
│  POST /api/v1/sample-size           │
│  POST /api/v1/rag/query             │
│  GET  /swagger-ui  ← OpenAPI UI     │
└──────────┬──────────────────────────┘
           │
    ┌──────┴──────────────────┐
    ▼                         ▼
┌──────────────┐    ┌─────────────────────┐
│ Расчётное    │    │ REGULATORY RAG      │
│ ядро         │    │ GigaChat API        │
│              │    │ ЕЭК №85 · EMA · FDA │
│ DesignEngine │    └─────────────────────┘
│ RSABEModule  │
│ PKModule     │    ┌─────────────────────┐
│ SampleSize   │    │ DOCUMENT GENERATOR  │
│ Engine       │    │ Markdown / JSON     │
└──────────────┘    └─────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│  PostgreSQL — проекты, расчёты      │
└─────────────────────────────────────┘
```

### Структура проекта

```
├── backend/                          # Kotlin + Quarkus 3.8 (Java 21)
│   ├── src/main/kotlin/com/bioequivalence/
│   │   ├── engine/
│   │   │   ├── DesignEngine.kt       # Выбор дизайна + RSABE + PK params
│   │   │   ├── SampleSizeEngine.kt   # Расчёт N (Owen's method + t-dist)
│   │   │   └── BECalculationService.kt  # Оркестратор
│   │   ├── api/
│   │   │   └── BEController.kt       # REST API (OpenAPI аннотации)
│   │   ├── document/
│   │   │   └── SynopsisGenerator.kt  # Markdown/JSON синопсис
│   │   ├── rag/
│   │   │   └── RagService.kt         # GigaChat OAuth2 + RAG
│   │   └── model/
│   │       └── Models.kt             # DTO / Entities
│   └── src/test/                     # JUnit 5 тесты
│
├── frontend/                         # React 18 + TypeScript + Tailwind
│   ├── src/
│   │   ├── components/
│   │   │   ├── CalculatorForm.tsx    # Форма ввода (react-hook-form + zod)
│   │   │   ├── ResultPanel.tsx       # Панель результатов
│   │   │   └── RagChat.tsx           # AI-чат интерфейс
│   │   ├── pages/
│   │   │   ├── Calculator.tsx        # Страница калькулятора
│   │   │   └── AiChat.tsx            # Страница AI-чата
│   │   └── utils/api.ts              # Axios API клиент
│
├── datasets/
│   ├── pk_reference_data.json        # ФК-данные 12 онкопрепаратов (PubMed)
│   ├── regulatory_qa_rag.json        # 12 Q&A для RAG обучения
│   └── sample_synopses.json          # 4 готовых синопсиса
│
├── experiments/
│   └── validate_calculations.py     # Python валидация расчётов
│
├── Roadmap.md                        # Детальный roadmap v1.0 → v2.5
├── docker-compose.yml                # Postgres + Backend + Frontend
└── SOLUTION.md                       # Этот файл
```

### Запуск

#### Через Docker Compose
```bash
# Опционально: настроить GigaChat API
export GIGACHAT_CLIENT_ID=your_client_id
export GIGACHAT_CLIENT_SECRET=your_secret

docker-compose up -d
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/swagger-ui
```

#### Локально
```bash
# Backend
cd backend
./gradlew quarkusDev

# Frontend
cd frontend
npm install && npm run dev
```

### Пример расчёта (README сценарий)

**Входные данные:**
- Онкопрепарат с CV_intra = 42%, t½ = 48ч, влияние пищи подтверждено

**Результат платформы:**
```json
{
  "studyDesign": "REPLICATE_DESIGN",
  "rsabe": {
    "decision": "APPLICABLE",
    "lowerBound": 70.21,
    "upperBound": 142.43
  },
  "pkParameters": {
    "washoutDays": 10,
    "accumulationRisk": true,
    "washoutWarning": "⚠️ t½ = 48ч. Washout = 10 дней."
  },
  "sampleSize": {
    "nRandomized": 72,
    "nScreened": 107
  },
  "foodEffect": "FASTING_AND_FED"
}
```

### Ключевые технические решения

1. **Детерминированность**: расчётное ядро не использует LLM — только математику (Owen's method, t-distribution)
2. **RSABE**: динамические границы по EMA формуле: |ln(GMR)|² ≤ k × σ̂_R² (k = 0.760)
3. **Washout**: 5×t½ по умолчанию, с предупреждениями для t½ > 24ч и t½ > 72ч
4. **GigaChat**: OAuth2 + fallback при отсутствии ключей (встроенные объяснения)
5. **RAG**: отдельный слой — не влияет на математику, только объяснения
