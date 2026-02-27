import { useState, useEffect } from 'react'
import type {
  TestingStatusResponse,
  ModelInfo,
  TestScenarioResult,
  PipelineError,
  DbMockResult,
  TestingSummary,
} from '../types/testing'
import { getTestingStatus, startModelTraining } from '../utils/api'

// ─── Status Badge Components ──────────────────────────────────────────────────

function TrainingStatusBadge({ status }: { status: string }) {
  const configs: Record<string, { label: string; cls: string }> = {
    TRAINED:              { label: '✅ Обучена',          cls: 'bg-green-100 text-green-800 border-green-300' },
    TRAINING_IN_PROGRESS: { label: '⏳ Обучается...',     cls: 'bg-blue-100 text-blue-800 border-blue-300' },
    NOT_TRAINED:          { label: '⚪ Не обучена',       cls: 'bg-gray-100 text-gray-600 border-gray-300' },
    FAILED:               { label: '❌ Ошибка обучения',  cls: 'bg-red-100 text-red-800 border-red-300' },
  }
  const { label, cls } = configs[status] ?? { label: status, cls: 'bg-gray-100 text-gray-600 border-gray-300' }
  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${cls}`}>
      {label}
    </span>
  )
}

function TestStatusBadge({ status }: { status: string }) {
  const configs: Record<string, { label: string; cls: string }> = {
    PASSED:      { label: '✅ Пройден',    cls: 'bg-green-100 text-green-800' },
    FAILED:      { label: '❌ Не пройден', cls: 'bg-red-100 text-red-800' },
    SKIPPED:     { label: '⏭️ Пропущен',   cls: 'bg-gray-100 text-gray-600' },
    IN_PROGRESS: { label: '⏳ Выполняется', cls: 'bg-blue-100 text-blue-800' },
  }
  const { label, cls } = configs[status] ?? { label: status, cls: 'bg-gray-100 text-gray-600' }
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${cls}`}>
      {label}
    </span>
  )
}

function DbStatusBadge({ status }: { status: string }) {
  const configs: Record<string, { label: string; cls: string }> = {
    READY:           { label: '✅ Готова',          cls: 'bg-green-100 text-green-800 border-green-300' },
    INITIALIZING:    { label: '⏳ Инициализация',   cls: 'bg-blue-100 text-blue-800 border-blue-300' },
    FAILED:          { label: '❌ Ошибка',           cls: 'bg-red-100 text-red-800 border-red-300' },
    NOT_INITIALIZED: { label: '⚪ Не инициализирована', cls: 'bg-gray-100 text-gray-600 border-gray-300' },
  }
  const { label, cls } = configs[status] ?? { label: status, cls: 'bg-gray-100 text-gray-600 border-gray-300' }
  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${cls}`}>
      {label}
    </span>
  )
}

// ─── Sub-sections ─────────────────────────────────────────────────────────────

function ModelSection({ model, onStartTraining, trainingLoading }: {
  model: ModelInfo
  onStartTraining: () => void
  trainingLoading: boolean
}) {
  return (
    <div className="bg-white border rounded-xl p-6 shadow-sm space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">🧠 Модель</h2>
        <TrainingStatusBadge status={model.status} />
      </div>

      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <p className="text-gray-500">Название</p>
          <p className="font-semibold">{model.modelName}</p>
        </div>
        <div>
          <p className="text-gray-500">Версия</p>
          <p className="font-semibold">{model.modelVersion}</p>
        </div>
      </div>

      {model.metrics && (
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">📊 Метрики</h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {model.metrics.accuracy != null && (
              <MetricTile label="Accuracy" value={`${(model.metrics.accuracy * 100).toFixed(2)}%`} />
            )}
            {model.metrics.r2Score != null && (
              <MetricTile label="R²" value={model.metrics.r2Score.toFixed(4)} />
            )}
            {model.metrics.rmse != null && (
              <MetricTile label="RMSE" value={model.metrics.rmse.toFixed(4)} />
            )}
            {model.metrics.mae != null && (
              <MetricTile label="MAE" value={model.metrics.mae.toFixed(4)} />
            )}
          </div>
          <div className="grid grid-cols-2 gap-3 mt-3 text-xs text-gray-500">
            {model.metrics.trainingDataPoints != null && (
              <span>Обучающих образцов: <strong>{model.metrics.trainingDataPoints}</strong></span>
            )}
            {model.metrics.validationDataPoints != null && (
              <span>Валидационных: <strong>{model.metrics.validationDataPoints}</strong></span>
            )}
            {model.metrics.trainedAt && (
              <span>Дата обучения: <strong>{model.metrics.trainedAt.slice(0, 10)}</strong></span>
            )}
          </div>
          {model.metrics.notes && (
            <p className="text-xs text-gray-500 mt-2 italic">{model.metrics.notes}</p>
          )}
        </div>
      )}

      {(model.datasetName || model.datasetUrl) && (
        <div className="border rounded-lg p-3 text-sm">
          <p className="text-gray-500 text-xs uppercase tracking-wide mb-1">Dataset</p>
          <p className="font-semibold">{model.datasetName}</p>
          {model.datasetUrl && (
            <a
              href={model.datasetUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline text-xs break-all"
            >
              {model.datasetUrl}
            </a>
          )}
          {model.datasetDescription && (
            <p className="text-gray-500 text-xs mt-1">{model.datasetDescription}</p>
          )}
        </div>
      )}

      {model.trainingMessage && (
        <p className="text-sm text-gray-500 italic">{model.trainingMessage}</p>
      )}

      {model.canStartTraining && (
        <button
          onClick={onStartTraining}
          disabled={trainingLoading}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-medium py-2 rounded-lg text-sm"
        >
          {trainingLoading ? '⏳ Запуск...' : '▶️ Запустить обучение'}
        </button>
      )}
    </div>
  )
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white rounded-lg p-3 border text-center">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="font-bold text-sm mt-1">{value}</p>
    </div>
  )
}

function SummarySection({ summary }: { summary: TestingSummary }) {
  const total = summary.totalTests || 1
  const passedPct = Math.round((summary.passed / total) * 100)
  return (
    <div className="bg-white border rounded-xl p-6 shadow-sm">
      <h2 className="text-lg font-bold text-gray-900 mb-4">📈 Сводка тестов</h2>
      <div className="grid grid-cols-4 gap-4 text-center">
        <div className="bg-gray-50 rounded-lg p-3">
          <p className="text-2xl font-bold text-gray-700">{summary.totalTests}</p>
          <p className="text-xs text-gray-500">Всего</p>
        </div>
        <div className="bg-green-50 rounded-lg p-3">
          <p className="text-2xl font-bold text-green-600">{summary.passed}</p>
          <p className="text-xs text-gray-500">Пройдено</p>
        </div>
        <div className="bg-red-50 rounded-lg p-3">
          <p className="text-2xl font-bold text-red-600">{summary.failed}</p>
          <p className="text-xs text-gray-500">Не пройдено</p>
        </div>
        <div className="bg-gray-50 rounded-lg p-3">
          <p className="text-2xl font-bold text-gray-500">{summary.skipped}</p>
          <p className="text-xs text-gray-500">Пропущено</p>
        </div>
      </div>
      <div className="mt-4">
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>Прохождение</span>
          <span>{passedPct}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div
            className={`h-2 rounded-full transition-all ${passedPct === 100 ? 'bg-green-500' : passedPct >= 75 ? 'bg-amber-400' : 'bg-red-500'}`}
            style={{ width: `${passedPct}%` }}
          />
        </div>
      </div>
      <p className="text-xs text-gray-400 mt-2">
        Запущено: {new Date(summary.runAt).toLocaleString('ru-RU')}
      </p>
    </div>
  )
}

function TestResultsSection({ results }: { results: TestScenarioResult[] }) {
  const [expanded, setExpanded] = useState<string | null>(null)

  return (
    <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b bg-gray-50">
        <h2 className="text-lg font-bold text-gray-900">🧪 Результаты тест-сценариев</h2>
        <p className="text-sm text-gray-500 mt-0.5">Практические врачебные сценарии (онкология)</p>
      </div>
      <div className="divide-y">
        {results.map((r) => (
          <div key={r.scenarioId} className="px-6 py-4">
            <div
              className="flex items-center justify-between cursor-pointer"
              onClick={() => setExpanded(expanded === r.scenarioId ? null : r.scenarioId)}
            >
              <div className="flex items-center gap-3">
                <span className="text-xs font-mono text-gray-400 w-10">{r.scenarioId}</span>
                <div>
                  <p className="font-medium text-gray-900 text-sm">{r.scenarioName}</p>
                  <p className="text-xs text-gray-500">{r.description}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 shrink-0 ml-4">
                <span className="text-xs text-gray-400">{r.executionTimeMs}ms</span>
                <TestStatusBadge status={r.status} />
                <span className="text-gray-400 text-xs">{expanded === r.scenarioId ? '▲' : '▼'}</span>
              </div>
            </div>

            {expanded === r.scenarioId && (
              <div className="mt-3 pl-13 text-xs space-y-2">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div className="bg-gray-50 rounded p-2">
                    <p className="text-gray-400 mb-1">Ожидалось</p>
                    <p className="font-mono text-gray-700 break-all">{r.expectedResult}</p>
                  </div>
                  <div className={`rounded p-2 ${r.status === 'PASSED' ? 'bg-green-50' : 'bg-red-50'}`}>
                    <p className="text-gray-400 mb-1">Фактически</p>
                    <p className="font-mono text-gray-700 break-all">{r.actualResult ?? '—'}</p>
                  </div>
                </div>
                {r.errorMessage && (
                  <div className="bg-red-50 border border-red-200 rounded p-2 text-red-700">
                    <strong>Ошибка:</strong> {r.errorMessage}
                  </div>
                )}
                <p className="text-gray-400">
                  Выполнено: {new Date(r.executedAt).toLocaleString('ru-RU')}
                </p>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

function PipelineSection({ errors }: { errors: PipelineError[] }) {
  return (
    <div className="bg-white border rounded-xl p-6 shadow-sm">
      <h2 className="text-lg font-bold text-gray-900 mb-4">
        ⚙️ Ошибки Pipeline
        {errors.length > 0 && (
          <span className="ml-2 bg-red-100 text-red-700 text-sm font-normal px-2 py-0.5 rounded-full">
            {errors.length}
          </span>
        )}
      </h2>
      {errors.length === 0 ? (
        <div className="text-center py-6 text-gray-400">
          <p className="text-3xl mb-2">✅</p>
          <p className="font-medium">Ошибок нет</p>
          <p className="text-sm">Все стадии pipeline работают корректно</p>
        </div>
      ) : (
        <div className="space-y-3">
          {errors.map((err, idx) => (
            <div
              key={idx}
              className={`border rounded-lg p-4 ${err.isCritical ? 'border-red-300 bg-red-50' : 'border-amber-200 bg-amber-50'}`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-semibold text-sm">{err.stage}</span>
                <span className={`text-xs font-mono px-2 py-0.5 rounded ${err.isCritical ? 'bg-red-200 text-red-800' : 'bg-amber-200 text-amber-800'}`}>
                  {err.errorCode}
                </span>
              </div>
              <p className="text-sm text-gray-700">{err.message}</p>
              {err.suggestion && (
                <p className="text-xs text-gray-500 mt-1 italic">💡 {err.suggestion}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">
                {new Date(err.timestamp).toLocaleString('ru-RU')}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function DbMockSection({ db }: { db: DbMockResult }) {
  return (
    <div className="bg-white border rounded-xl p-6 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-900">🗄️ Готовность БД (Mock)</h2>
        <DbStatusBadge status={db.status} />
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
        <div className={`rounded-lg p-3 border text-center ${db.writeTestPassed ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
          <p className="text-xs text-gray-500">Запись</p>
          <p className="font-bold text-lg">{db.writeTestPassed ? '✅' : '❌'}</p>
        </div>
        <div className={`rounded-lg p-3 border text-center ${db.readTestPassed ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}`}>
          <p className="text-xs text-gray-500">Чтение</p>
          <p className="font-bold text-lg">{db.readTestPassed ? '✅' : '❌'}</p>
        </div>
        <div className="bg-gray-50 rounded-lg p-3 border text-center">
          <p className="text-xs text-gray-500">Записей</p>
          <p className="font-bold text-lg">{db.recordCount}</p>
        </div>
        <div className="bg-gray-50 rounded-lg p-3 border text-center">
          <p className="text-xs text-gray-500">Время подключения</p>
          <p className="font-bold text-lg">{db.connectionTimeMs}ms</p>
        </div>
      </div>
      <p className="text-sm text-gray-600">{db.message}</p>
    </div>
  )
}

// ─── Main Testing Page ────────────────────────────────────────────────────────

export default function Testing() {
  const [status, setStatus] = useState<TestingStatusResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [trainingLoading, setTrainingLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [trainingMessage, setTrainingMessage] = useState<string | null>(null)

  const loadStatus = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getTestingStatus()
      setStatus(data)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err)
      setError(`Ошибка загрузки: ${msg}. Убедитесь, что backend запущен на порту 8080.`)
    } finally {
      setLoading(false)
    }
  }

  const handleStartTraining = async () => {
    setTrainingLoading(true)
    setTrainingMessage(null)
    try {
      const result = await startModelTraining()
      setTrainingMessage(result.message)
      await loadStatus()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err)
      setTrainingMessage(`Ошибка запуска обучения: ${msg}`)
    } finally {
      setTrainingLoading(false)
    }
  }

  useEffect(() => { loadStatus() }, [])

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">🧪 Тестирование</h1>
          <p className="text-gray-500 text-sm mt-1">
            Статус модели · Результаты тестов · Ошибки pipeline · Готовность БД
          </p>
        </div>
        <button
          onClick={loadStatus}
          disabled={loading}
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg text-sm font-medium"
        >
          {loading ? '⏳ Загрузка...' : '🔄 Обновить'}
        </button>
      </div>

      {trainingMessage && (
        <div className="mb-4 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl p-4 text-sm">
          {trainingMessage}
        </div>
      )}

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 text-sm">
          {error}
        </div>
      )}

      {loading && !status && (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3 animate-pulse">⏳</p>
          <p>Запуск тестов...</p>
        </div>
      )}

      {status && (
        <div className="space-y-6">
          {/* Top row: Model + Summary */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <ModelSection
              model={status.model}
              onStartTraining={handleStartTraining}
              trainingLoading={trainingLoading}
            />
            <SummarySection summary={status.summary} />
          </div>

          {/* DB Mock */}
          <DbMockSection db={status.dbMock} />

          {/* Pipeline Errors */}
          <PipelineSection errors={status.pipelineErrors} />

          {/* Test Results */}
          <TestResultsSection results={status.testResults} />

          <p className="text-xs text-gray-400 text-center">
            Сгенерировано: {new Date(status.generatedAt).toLocaleString('ru-RU')}
          </p>
        </div>
      )}
    </div>
  )
}
