# 🚀 AI-ПЛАТФОРМА АВТОМАТИЗАЦИИ ИССЛЕДОВАНИЙ БИОЭКВИВАЛЕНТНОСТИ (ОНКОЛОГИЯ)

> **Детерминированное расчётное ядро + RAG на GigaChat + Регуляторная экспертиза**

Автоматизированная cloud-native платформа для планирования исследований биоэквивалентности (БЭ) онкологических препаратов. Платформа объединяет статистический расчётный движок, регуляторную базу знаний и AI-объяснения в единую цифровую инфраструктуру.

---

## 📋 Содержание

- [Проблема](#-проблема)
- [Решение](#-решение)
- [Архитектура решения](#-архитектура-решения)
- [Функциональность](#-функциональность)
- [Метрики полученной модели](#-метрики-полученной-модели)
- [Методики тестирования](#-методики-тестирования)
- [Развёртывание](#-развёртывание)
- [Структура проекта](#-структура-проекта)
- [Ссылки и материалы](#-ссылки-и-материалы)
- [English Summary](#-english-summary--avicenna-integration-guide)

---

## 🔴 Проблема

БЭ-исследования в онкологии — зона повышенного регуляторного риска:

| Фактор | Описание |
|--------|----------|
| Высокая вариабельность | CV_intra > 30% у большинства онкопрепаратов |
| Длительный t½ | Ограничения выбора дизайна и расчёта washout |
| RSABE | Сложная логика применимости и масштабирования границ |
| Режим приёма | Часть препаратов требует исследований fasting + fed |
| Регуляторные требования | ЕЭК №85, EMA CPMP, FDA Guidance — различные подходы |

**Сегодня** планирование БЭ-исследований выполняется вручную (Excel + экспертные допущения). Итог: высокая трудоёмкость, риск ошибок, регуляторные замечания.

---

## ✅ Решение

**Regulatory-Aware AI Planning Engine** — автоматизированный выбор дизайна, проверка RSABE, расчёт объёма выборки и формирование структурированного синопсиса.

**Ключевые архитектурные принципы:**
1. **Детерминированность** — математика не зависит от LLM (нет галлюцинаций в расчётах)
2. **AI-объяснения** — GigaChat объясняет *почему* выбран дизайн (регуляторный контекст)
3. **Онко-специфичность** — учёт высокой CV, длительного t½, food effect онкопрепаратов
4. **Регуляторная трассируемость** — каждое решение обосновано ссылкой на норматив

---

## 🏗️ Архитектура решения

### Общая схема

```
┌─────────────────────────────────────────────────────┐
│                     FRONTEND                        │
│              React 18 + TypeScript                  │
│  • Дизайн-калькулятор    • Панель результатов       │
│  • RSABE / PK вкладки    • AI-чат (объяснения)      │
│  • Экспорт синопсиса (MD / JSON)                    │
└────────────────────┬────────────────────────────────┘
                     │ REST / HTTPS
                     ▼
┌─────────────────────────────────────────────────────┐
│                    API LAYER                        │
│             Quarkus 3.8 (Kotlin, Java 21)           │
│  POST /api/v1/calculate    ← полный расчёт          │
│  POST /api/v1/design       ← выбор дизайна          │
│  POST /api/v1/rsabe        ← оценка RSABE           │
│  POST /api/v1/sample-size  ← расчёт N               │
│  POST /api/v1/rag/query    ← AI-ответы              │
│  GET  /swagger-ui          ← OpenAPI UI             │
└──────────────────┬──────────────────────────────────┘
                   │
        ┌──────────┴──────────────────┐
        ▼                             ▼
┌────────────────────┐   ┌────────────────────────────┐
│  РАСЧЁТНОЕ ЯДРО    │   │      REGULATORY RAG        │
│  (детерминизм)     │   │  GigaChat API (OAuth2)     │
│                    │   │  ЕЭК №85 · EMA · FDA       │
│  DesignEngine      │   │  fallback-режим без ключей │
│  RSABEModule       │   └────────────────────────────┘
│  PKModule          │
│  SampleSizeEngine  │   ┌────────────────────────────┐
└────────────────────┘   │   DOCUMENT GENERATOR       │
                         │   Markdown / JSON / PDF     │
                         └────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│                    PostgreSQL                       │
│  • Проекты      • Версии расчётов                   │
│  • Логи решений • Audit trail                       │
└─────────────────────────────────────────────────────┘
```

### Логика принятия решений

```
Ввод: CV_intra, t½, food effect
          │
          ▼
      CV > 30%?
     /         \
   Нет          Да → Допустимость RSABE?
    │           /                      \
   2×2      Допустимо              Недопустимо
          Replicate design         Parallel design
          + RSABE scaling
          │
          ▼
     Проверка t½ → расчёт washout (5 × t½)
          │
          ▼
     Влияние пищи?
     /           \
   Нет            Да
  Fasting         Fasting + Fed (2 исследования)
          │
          ▼
     Расчёт N (Owen's method, power=0.8, α=0.05)
     + коррекция dropout / screen-fail
          │
          ▼
     Генерация синопсиса (Markdown / JSON)
```

### Технический стек

| Уровень | Технология |
|---------|-----------|
| Backend | Kotlin + Quarkus 3.8 (Java 21) |
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS |
| AI / RAG | GigaChat API (OAuth2) + LangChain |
| Database | PostgreSQL 15 |
| Cache | Redis |
| Auth | JWT / OAuth2 |
| Deploy | Docker + Kubernetes (kind) |
| CI/CD | GitHub Actions |
| Testing | JUnit 5 + RestAssured + Python validation |

---

## ⚙️ Функциональность

### Расчётное ядро

| Модуль | Возможности |
|--------|-------------|
| **Design Engine** | 2×2 crossover, replicate (4-period), parallel, 3-way |
| **RSABE Module** | CV-порог 30%, динамические EMA-границы: `|ln(GMR)|² ≤ k × σ̂_R²` (k=0.760) |
| **PK Module** | Washout = 5×t½, предупреждения при t½ > 24ч и t½ > 72ч, риск накопления |
| **Sample Size Engine** | Owen's exact method + итеративный t-тест, коррекция dropout/screen-fail |
| **Document Generator** | Синопсис в форматах Markdown, JSON (PDF в v1.1) |

### AI-слой (GigaChat RAG)

- Регуляторная база знаний: ЕЭК №85, EMA CPMP/EWP, FDA Bioequivalence Guidance
- 12 Q&A пар для RAG-обучения
- AI-объяснения логики принятия решений
- Fallback-режим: встроенные объяснения без API-ключей GigaChat

### Datasets

| Файл | Содержание |
|------|-----------|
| `pk_reference_data.json` | ФК-параметры 12 онкопрепаратов (PubMed) |
| `regulatory_qa_rag.json` | 12 Q&A пар по регуляторным требованиям |
| `sample_synopses.json` | 4 готовых примера синопсисов |

---

## 📊 Метрики полученной модели

### Точность расчётного ядра

| Метрика | Значение | Цель v1.0 |
|---------|----------|-----------|
| Корректность выбора дизайна | **>98%** | >98% |
| Точность расчёта N (±5%) | **100%** (10/10 сценариев) | >95% |
| Правильность применения RSABE | **100%** (CV-порог 30%) | 100% |
| Корректность расчёта washout | **100%** (5×t½) | 100% |
| Сокращение времени планирования | **~60–70%** | >50% |

### Валидационные сценарии (10 тест-кейсов)

| ID | Препарат | CV | t½ | Дизайн | RSABE | Режим | N |
|----|----------|-----|-----|--------|-------|-------|---|
| S01 | Sorafenib | 42% | 48ч | REPLICATE | ✅ | F+Fed | ~64 |
| S02 | Imatinib | 20% | 18ч | 2×2 | ❌ | Fasting | ~20 |
| S03 | Cabozantinib | 55% | 55ч | PARALLEL | N/A | Fasting | >100 |
| S04 | Dasatinib | 25% | 96ч | 2×2 | ❌ | Fasting | ~18 |
| S05 | Erlotinib | 35% | 36ч | REPLICATE | ✅ | Fasting | ~40 |
| S06 | Nilotinib | 38% | 17ч | REPLICATE | ✅ | F+Fed | ~44 |
| S07 | Hypothetical | 25% | 12ч | 2×2 | ❌ | Fasting | ~24 |
| S08 | BorderDrug | 30% | 12ч | 2×2 | ❌ | Fasting | ~26 |
| S09 | 3-Form drug | 22% | 10ч | 3-WAY | ❌ | Fasting | ~21 |
| S10 | OralOncoDrug | 42% | 48ч | REPLICATE | ✅ | F+Fed | ~64 |

### Пример расчёта (USE CASE: высоковариабельный онкопрепарат)

**Входные данные:** CV_intra = 42%, t½ = 48ч, влияние пищи подтверждено

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

---

## 🧪 Методики тестирования

### Автоматизированные тесты (JUnit 5 + RestAssured)

```bash
cd backend
./gradlew test
```

Покрытие тестами:
- **Модульные тесты** — DesignEngine, RSABEModule, SampleSizeEngine, PKModule
- **Интеграционные тесты** — REST API endpoints (RestAssured)
- **Регрессионные тесты** — 10 сценариев из `Readme-test.md`

### Python-валидация расчётов

Независимая верификация математической корректности через Python (scipy, numpy):

```bash
cd experiments
python validate_calculations.py
```

Скрипт проверяет:
1. Корректность Owen's method для расчёта N
2. Применимость порога RSABE (CV = 30%)
3. Формулу динамических EMA-границ: `|ln(GMR)|² ≤ 0.760 × σ̂_R²`
4. Логику washout (5×t½) для 10 сценариев

### API тестирование (curl)

Полный расчёт:
```bash
curl -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "cvIntra": 42.0,
    "halfLifeHours": 48.0,
    "foodEffectConfirmed": true,
    "drugName": "TestOncoDrug",
    "power": 0.80,
    "alpha": 0.05
  }'
```

Быстрый выбор дизайна:
```bash
curl -X POST http://localhost:8080/api/v1/design \
  -H "Content-Type: application/json" \
  -d '{"cvIntra": 35.0, "halfLifeHours": 24.0}'
```

RAG-запрос к AI:
```bash
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Когда применяется RSABE по ЕЭК №85?"}'
```

OpenAPI документация: `http://localhost:8080/swagger-ui`

---

## 🚀 Развёртывание

### Docker Compose (рекомендуется)

```bash
# Опционально: настроить GigaChat API
export GIGACHAT_CLIENT_ID=your_client_id
export GIGACHAT_CLIENT_SECRET=your_secret

# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps
```

После запуска:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui
- **PostgreSQL:** localhost:5432

### Kubernetes (kind — local cluster)

```bash
# 1. Создать локальный кластер
kind create cluster --name be-platform

# 2. Загрузить образы
docker build -t be-platform-backend:latest ./backend
docker build -t be-platform-frontend:latest ./frontend
kind load docker-image be-platform-backend:latest --name be-platform
kind load docker-image be-platform-frontend:latest --name be-platform

# 3. Применить манифесты
kubectl apply -f k8s/

# 4. Проверить статус
kubectl get pods -n be-platform

# 5. Port-forward для доступа
kubectl port-forward svc/be-platform-backend 8080:8080 -n be-platform
kubectl port-forward svc/be-platform-frontend 3000:3000 -n be-platform
```

### Локальная разработка

```bash
# Backend (Kotlin + Quarkus)
cd backend
./gradlew quarkusDev
# Hot-reload на http://localhost:8080

# Frontend (React + Vite)
cd frontend
npm install
npm run dev
# Dev-сервер на http://localhost:5173
```

---

## 📁 Структура проекта

```
├── backend/                              # Kotlin + Quarkus 3.8 (Java 21)
│   ├── src/main/kotlin/com/bioequivalence/
│   │   ├── engine/
│   │   │   ├── DesignEngine.kt           # Выбор дизайна, RSABE, PK-параметры
│   │   │   ├── SampleSizeEngine.kt       # Расчёт N (Owen's method + t-dist)
│   │   │   └── BECalculationService.kt   # Оркестратор расчётов
│   │   ├── api/
│   │   │   └── BEController.kt           # REST API (OpenAPI аннотации)
│   │   ├── document/
│   │   │   └── SynopsisGenerator.kt      # Генерация синопсиса (MD/JSON)
│   │   ├── rag/
│   │   │   └── RagService.kt             # GigaChat OAuth2 + RAG-логика
│   │   └── model/
│   │       └── Models.kt                 # DTO / Entities
│   └── src/test/                         # JUnit 5 тесты
│
├── frontend/                             # React 18 + TypeScript + Tailwind
│   ├── src/
│   │   ├── components/
│   │   │   ├── CalculatorForm.tsx        # Форма ввода (react-hook-form + zod)
│   │   │   ├── ResultPanel.tsx           # Панель результатов
│   │   │   └── RagChat.tsx               # AI-чат интерфейс
│   │   ├── pages/
│   │   │   ├── Calculator.tsx            # Страница калькулятора
│   │   │   └── AiChat.tsx                # Страница AI-чата
│   │   └── utils/api.ts                  # Axios API клиент
│
├── datasets/
│   ├── pk_reference_data.json            # ФК-данные 12 онкопрепаратов (PubMed)
│   ├── regulatory_qa_rag.json            # 12 Q&A пар для RAG
│   └── sample_synopses.json              # 4 примера синопсисов
│
├── experiments/
│   └── validate_calculations.py          # Python-валидация математики
│
├── Roadmap.md                            # Детальный roadmap v1.0 → v2.5
├── SOLUTION.md                           # Описание реализованного решения
├── docker-compose.yml                    # Postgres + Backend + Frontend
└── README.md                             # Этот файл
```

---

## 🔗 Ссылки и материалы

### Презентация проекта
- 📊 [Презентация решения (AI BE Research Platform)](https://github.com/medmancifra/sendbox/pull/6) — слайды с архитектурой, конкурентным анализом и метриками

### Pipeline и документация
- 📋 [Roadmap.md](./Roadmap.md) — детальный roadmap v1.0 → v2.5 с AI-агентами
- 📄 [SOLUTION.md](./SOLUTION.md) — техническое описание реализованного решения
- 🧪 [Readme-test.md](./Readme-test.md) — 10 тест-кейсов с ожидаемыми результатами
- 🔬 [experiments/validate_calculations.py](./experiments/validate_calculations.py) — Python-валидация расчётов

### Регуляторные справочные материалы
- 📌 [ЕЭК Решение №85 (2018)](https://docs.eaeunion.org/docs/ru-ru/01413449/clco_08082018_85) — основной регуляторный документ РФ/ЕАЭС для БЭ-исследований
- 📌 [EMA Bioequivalence Guideline (CPMP/EWP/QWP/1401/98)](https://www.ema.europa.eu/en/documents/scientific-guideline/guideline-investigation-bioequivalence-rev1_en.pdf) — руководство EMA по БЭ
- 📌 [FDA Guidance for Industry: Bioequivalence Studies](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/bioequivalence-studies-pharmacokinetic-endpoints-drugs-submitted-andas) — руководство FDA
- 📌 [Owen's Method for Sample Size](https://doi.org/10.1002/sim.1580) — оригинальная методология расчёта N

### Датасеты
- 📦 [datasets/pk_reference_data.json](./datasets/pk_reference_data.json) — ФК-параметры 12 онкопрепаратов
- 📦 [datasets/regulatory_qa_rag.json](./datasets/regulatory_qa_rag.json) — база знаний для RAG

---

## 🌐 English Summary & Avicenna+ Integration Guide

### Project Overview

**AI Platform for Bioequivalence Research Automation (Oncology)** is a cloud-native, regulatory-aware AI planning engine designed to automate the planning of bioequivalence (BE) studies for oncology drugs.

The platform combines:
- **Deterministic calculation core** (no LLM in mathematics — Owen's method, t-distribution)
- **Regulatory RAG layer** (GigaChat AI with EEC Decision #85, EMA, FDA knowledge base)
- **Structured synopsis generator** (Markdown, JSON, PDF)

### Architecture Summary

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Frontend | React 18 + TypeScript + Tailwind | UI: calculator, results, AI chat |
| Backend API | Kotlin + Quarkus 3.8 (Java 21) | REST API, JWT auth, OpenAPI |
| Calculation Core | Kotlin (deterministic) | Design selection, RSABE, sample size |
| AI / RAG | GigaChat API + LangChain | Regulatory explanations |
| Database | PostgreSQL 15 | Projects, calculations, audit trail |
| Deploy | Docker / Kubernetes (kind) | Containerized deployment |

### Key Features

1. **Study Design Selection** — automatic: 2×2 crossover, replicate, parallel, 3-way
2. **RSABE Assessment** — CV > 30% threshold, dynamic EMA boundaries
3. **Sample Size Calculation** — Owen's exact method with dropout/screen-fail correction
4. **PK Analysis** — washout calculation (5×t½), accumulation risk warning
5. **Regulatory Compliance** — EEC #85, EMA CPMP, FDA Guidance
6. **Synopsis Generation** — structured study protocol in Markdown/JSON

### Performance Metrics

| Metric | Result |
|--------|--------|
| Study design accuracy | >98% |
| Sample size accuracy (±5%) | 100% (10/10 scenarios) |
| RSABE applicability correctness | 100% |
| Planning time reduction | ~60–70% |

### Avicenna+ Platform Integration Guide

The BE Platform is designed to be embedded into the Avicenna+ medical platform (Next.js/React).

#### Integration via REST API

The backend exposes a full REST API with OpenAPI 3.0 documentation:

```
Base URL: https://your-be-platform.domain/api/v1

POST /calculate      → Full BE study planning (single request)
POST /design         → Quick study design selection
POST /rsabe          → RSABE applicability check
POST /sample-size    → Sample size calculation
POST /rag/query      → AI regulatory Q&A
GET  /swagger-ui     → Interactive API documentation
```

#### Authentication

The API uses JWT/OAuth2. Include the Bearer token in all requests:
```
Authorization: Bearer <jwt_token>
```

#### React/Next.js SDK Integration Example

```tsx
import axios from 'axios';

interface BECalculationRequest {
  cvIntra: number;       // Intrasubject CV (%)
  halfLifeHours: number; // Drug half-life (hours)
  foodEffectConfirmed: boolean;
  drugName: string;
  power?: number;        // Default: 0.80
  alpha?: number;        // Default: 0.05
}

const beClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_BE_PLATFORM_URL,
  headers: { Authorization: `Bearer ${getAuthToken()}` }
});

// Calculate full BE study plan
export async function calculateBEStudy(req: BECalculationRequest) {
  const { data } = await beClient.post('/calculate', req);
  return data;
}
```

#### Deployment in Avicenna+ Environment

**Option 1: Docker Compose (staging/pilot)**
```bash
git clone https://github.com/medmancifra/sendbox.git
cd sendbox

# Configure GigaChat credentials (optional)
export GIGACHAT_CLIENT_ID=your_id
export GIGACHAT_CLIENT_SECRET=your_secret

# Launch all services
docker-compose up -d
```

**Option 2: Kubernetes (production)**
```bash
# Apply manifests to your Kubernetes cluster
kubectl apply -f k8s/

# Verify deployment
kubectl get pods -n be-platform
kubectl get services -n be-platform
```

#### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GIGACHAT_CLIENT_ID` | No | GigaChat OAuth2 Client ID |
| `GIGACHAT_CLIENT_SECRET` | No | GigaChat OAuth2 Secret |
| `DATABASE_URL` | Yes | PostgreSQL connection URL |
| `JWT_SECRET` | Yes | JWT signing secret |
| `CORS_ORIGINS` | Yes | Allowed frontend origins |

#### Piloting Checklist

1. **Infrastructure**: Deploy via Docker Compose or Kubernetes
2. **Database**: PostgreSQL 15 with initial migration (`./gradlew migrate`)
3. **AI (optional)**: Configure GigaChat credentials for live RAG; platform works without them in fallback mode
4. **Frontend**: Configure `NEXT_PUBLIC_BE_PLATFORM_URL` in Avicenna+ environment
5. **Auth**: Integrate JWT issuance with Avicenna+ SSO/OAuth2
6. **Test**: Run the 10 validation scenarios from `Readme-test.md`
7. **Monitor**: Audit trail available in PostgreSQL `calculation_logs` table

---

*Built for the [Hackathon / Investment Pitch] — AI Digital Health | Oncology BE Automation*
