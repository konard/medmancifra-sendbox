import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { BECalculationResponse } from '../types/api'

interface Props {
  result: BECalculationResponse
}

const designLabels: Record<string, string> = {
  TWO_WAY_CROSSOVER: '2×2 Crossover',
  REPLICATE_DESIGN: 'Реплицированный дизайн (4-периодный)',
  PARALLEL_DESIGN: 'Параллельный дизайн',
  THREE_WAY_CROSSOVER: '3-way Crossover',
}

const foodLabels: Record<string, string> = {
  NONE: 'Не оценивается',
  FASTING_ONLY: 'Натощак',
  FASTING_AND_FED: 'Натощак + после еды',
}

const rsabeColors: Record<string, string> = {
  NOT_APPLICABLE: 'bg-green-100 text-green-800 border-green-300',
  APPLICABLE: 'bg-amber-100 text-amber-800 border-amber-300',
  REQUIRED: 'bg-orange-100 text-orange-800 border-orange-300',
  NOT_ALLOWED: 'bg-red-100 text-red-800 border-red-300',
}

export default function ResultPanel({ result }: Props) {
  const { sampleSize, rsabe, pkParameters, foodEffect, synopsis, aiExplanation, studyDesign } = result

  const downloadMarkdown = () => {
    const blob = new Blob([synopsis.markdownExport], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `synopsis-${result.drugName}-${new Date().toISOString().slice(0, 10)}.md`
    a.click()
    URL.revokeObjectURL(url)
  }

  const downloadJson = () => {
    const blob = new Blob([JSON.stringify(synopsis.jsonExport, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `synopsis-${result.drugName}-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 text-white rounded-xl p-6">
        <h2 className="text-2xl font-bold mb-1">✅ {result.drugName}</h2>
        <p className="text-blue-100 text-sm">{result.regulatoryVersion}</p>
        <p className="text-blue-200 text-xs mt-1">ID: {result.requestId}</p>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <MetricCard
          label="Дизайн"
          value={designLabels[studyDesign] || studyDesign}
          icon="🔬"
          color="blue"
        />
        <MetricCard
          label="N рандомизации"
          value={sampleSize.nRandomized.toString()}
          icon="👥"
          color="green"
        />
        <MetricCard
          label="N скрининга"
          value={sampleSize.nScreened.toString()}
          icon="📋"
          color="purple"
        />
        <MetricCard
          label="Режим приёма"
          value={foodLabels[foodEffect] || foodEffect}
          icon="🍽️"
          color="orange"
        />
      </div>

      {/* RSABE */}
      <div className={`border rounded-xl p-4 ${rsabeColors[rsabe.decision]}`}>
        <h3 className="font-semibold mb-2">RSABE: {rsabe.decision}</h3>
        <div className="grid grid-cols-3 gap-3 mb-2 text-sm">
          <div><span className="opacity-70">CV_intra:</span> <strong>{rsabe.cvIntra.toFixed(1)}%</strong></div>
          <div><span className="opacity-70">Нижняя граница:</span> <strong>{rsabe.lowerBound.toFixed(2)}%</strong></div>
          <div><span className="opacity-70">Верхняя граница:</span> <strong>{rsabe.upperBound.toFixed(2)}%</strong></div>
        </div>
        <p className="text-xs opacity-80">{rsabe.justification}</p>
      </div>

      {/* PK Parameters */}
      <div className="bg-gray-50 rounded-xl p-4">
        <h3 className="font-semibold text-gray-700 mb-3">⚗️ Фармакокинетика</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <div className="bg-white rounded-lg p-3 border">
            <p className="text-gray-500 text-xs">t½</p>
            <p className="font-bold text-lg">{pkParameters.halfLife}ч</p>
          </div>
          <div className="bg-white rounded-lg p-3 border">
            <p className="text-gray-500 text-xs">Washout</p>
            <p className="font-bold text-lg">{pkParameters.washoutDays.toFixed(1)}д</p>
          </div>
          <div className="bg-white rounded-lg p-3 border">
            <p className="text-gray-500 text-xs">Washout (часы)</p>
            <p className="font-bold text-lg">{pkParameters.washoutHours.toFixed(0)}ч</p>
          </div>
          <div className={`rounded-lg p-3 border ${pkParameters.accumulationRisk ? 'bg-amber-50 border-amber-200' : 'bg-white'}`}>
            <p className="text-gray-500 text-xs">Риск накопления</p>
            <p className="font-bold text-lg">{pkParameters.accumulationRisk ? '⚠️ Да' : '✅ Нет'}</p>
          </div>
        </div>
        {pkParameters.washoutWarning && (
          <div className="mt-3 bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
            {pkParameters.washoutWarning}
          </div>
        )}
      </div>

      {/* Sample Size Details */}
      <div className="bg-white border rounded-xl p-4">
        <h3 className="font-semibold text-gray-700 mb-3">📊 Расчёт выборки</h3>
        <div className="grid grid-cols-3 gap-4 mb-3">
          <div className="text-center">
            <p className="text-3xl font-bold text-blue-600">{sampleSize.nPerSequence}</p>
            <p className="text-xs text-gray-500">на последовательность</p>
          </div>
          <div className="text-center">
            <p className="text-3xl font-bold text-green-600">{sampleSize.nRandomized}</p>
            <p className="text-xs text-gray-500">N рандомизации</p>
          </div>
          <div className="text-center">
            <p className="text-3xl font-bold text-purple-600">{sampleSize.nScreened}</p>
            <p className="text-xs text-gray-500">N скрининга</p>
          </div>
        </div>
        <p className="text-xs text-gray-400 border-t pt-2">{sampleSize.calculation}</p>
      </div>

      {/* AI Explanation */}
      {aiExplanation && (
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <h3 className="font-semibold text-blue-800 mb-2">🤖 AI-объяснение (GigaChat RAG)</h3>
          <p className="text-sm text-blue-900">{aiExplanation}</p>
        </div>
      )}

      {/* Synopsis & Export */}
      <div className="bg-white border rounded-xl overflow-hidden">
        <div className="bg-gray-50 border-b px-4 py-3 flex items-center justify-between">
          <h3 className="font-semibold text-gray-700">📄 Синопсис исследования</h3>
          <div className="flex gap-2">
            <button
              onClick={downloadMarkdown}
              className="bg-gray-600 hover:bg-gray-700 text-white text-xs px-3 py-1.5 rounded-lg flex items-center gap-1"
            >
              ⬇️ Markdown
            </button>
            <button
              onClick={downloadJson}
              className="bg-gray-600 hover:bg-gray-700 text-white text-xs px-3 py-1.5 rounded-lg flex items-center gap-1"
            >
              ⬇️ JSON
            </button>
          </div>
        </div>
        <div className="p-6 prose prose-sm max-w-none">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {synopsis.markdownExport}
          </ReactMarkdown>
        </div>
      </div>
    </div>
  )
}

interface MetricCardProps {
  label: string
  value: string
  icon: string
  color: 'blue' | 'green' | 'purple' | 'orange'
}

const colorMap = {
  blue: 'bg-blue-50 border-blue-200',
  green: 'bg-green-50 border-green-200',
  purple: 'bg-purple-50 border-purple-200',
  orange: 'bg-orange-50 border-orange-200',
}

function MetricCard({ label, value, icon, color }: MetricCardProps) {
  return (
    <div className={`border rounded-xl p-4 ${colorMap[color]}`}>
      <p className="text-2xl mb-1">{icon}</p>
      <p className="text-xs text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="font-bold text-sm mt-1 leading-tight">{value}</p>
    </div>
  )
}
