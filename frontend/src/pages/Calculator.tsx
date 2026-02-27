import { useState } from 'react'
import CalculatorForm from '../components/CalculatorForm'
import ResultPanel from '../components/ResultPanel'
import type { BECalculationRequest, BECalculationResponse } from '../types/api'
import { calculate } from '../utils/api'

export default function Calculator() {
  const [result, setResult] = useState<BECalculationResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (data: BECalculationRequest) => {
    setLoading(true)
    setError(null)
    try {
      const res = await calculate(data)
      setResult(res)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err)
      setError(`Ошибка расчёта: ${msg}. Убедитесь, что backend запущен на порту 8080.`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          🔬 Дизайн-калькулятор БЭ исследования
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          Детерминированный расчёт дизайна, RSABE, объёма выборки и генерация синопсиса
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Form */}
        <div className="bg-white border rounded-xl p-6 shadow-sm">
          <CalculatorForm onSubmit={handleSubmit} loading={loading} />
        </div>

        {/* Results */}
        <div>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 mb-4">
              {error}
            </div>
          )}
          {result ? (
            <ResultPanel result={result} />
          ) : (
            <div className="bg-gray-50 border-2 border-dashed border-gray-200 rounded-xl p-8 text-center h-full flex flex-col items-center justify-center">
              <p className="text-4xl mb-3">🧪</p>
              <p className="text-gray-400 font-medium">Результаты появятся здесь</p>
              <p className="text-gray-300 text-sm mt-1">Заполните форму и нажмите "Рассчитать"</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
