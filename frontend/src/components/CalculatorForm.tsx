import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { BECalculationRequest } from '../types/api'

const schema = z.object({
  drugName: z.string().min(1, 'Укажите название препарата'),
  cvIntra: z.coerce.number().min(0.1).max(200),
  halfLife: z.coerce.number().min(0.1),
  power: z.coerce.number().min(0.5).max(0.99).default(0.80),
  alpha: z.coerce.number().min(0.01).max(0.2).default(0.05),
  dropoutRate: z.coerce.number().min(0).max(0.5).default(0.15),
  screenFailRate: z.coerce.number().min(0).max(0.5).default(0.20),
  foodEffect: z.boolean().default(false),
  notes: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

interface Props {
  onSubmit: (data: BECalculationRequest) => void
  loading: boolean
}

export default function CalculatorForm({ onSubmit, loading }: Props) {
  const { register, handleSubmit, formState: { errors }, watch } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      power: 0.80,
      alpha: 0.05,
      dropoutRate: 0.15,
      screenFailRate: 0.20,
      foodEffect: false,
    },
  })

  const cvIntra = watch('cvIntra')

  const getCVHint = (cv: number) => {
    if (!cv) return null
    if (cv <= 30) return { color: 'text-green-700 bg-green-50', text: '≤30% — стандартный 2×2 crossover' }
    if (cv <= 50) return { color: 'text-amber-700 bg-amber-50', text: '>30% — высоковариабельный препарат (RSABE)' }
    return { color: 'text-red-700 bg-red-50', text: '>50% — крайняя вариабельность (параллельный дизайн)' }
  }

  const cvHint = getCVHint(cvIntra)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Drug Info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Название препарата *
          </label>
          <input
            {...register('drugName')}
            placeholder="например, Сорафениб"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
          {errors.drugName && (
            <p className="text-red-500 text-xs mt-1">{errors.drugName.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            CV_intra (%) — внутрисубъектная вариабельность *
          </label>
          <input
            {...register('cvIntra')}
            type="number"
            step="0.1"
            placeholder="например, 42"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
          />
          {cvHint && (
            <p className={`text-xs mt-1 px-2 py-1 rounded ${cvHint.color}`}>
              {cvHint.text}
            </p>
          )}
          {errors.cvIntra && (
            <p className="text-red-500 text-xs mt-1">{errors.cvIntra.message}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            t½ (часы) — период полувыведения *
          </label>
          <input
            {...register('halfLife')}
            type="number"
            step="0.5"
            placeholder="например, 48"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
          />
          {errors.halfLife && (
            <p className="text-red-500 text-xs mt-1">{errors.halfLife.message}</p>
          )}
        </div>
      </div>

      {/* Statistical Parameters */}
      <div className="border-t pt-4">
        <h3 className="text-sm font-semibold text-gray-600 mb-3 uppercase tracking-wide">
          Статистические параметры
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Мощность (β)
            </label>
            <select
              {...register('power')}
              className="w-full border border-gray-300 rounded-lg px-2 py-2 text-sm"
            >
              <option value="0.80">80%</option>
              <option value="0.90">90%</option>
              <option value="0.95">95%</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Уровень значимости (α)
            </label>
            <select
              {...register('alpha')}
              className="w-full border border-gray-300 rounded-lg px-2 py-2 text-sm"
            >
              <option value="0.05">5%</option>
              <option value="0.025">2.5%</option>
              <option value="0.10">10%</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Drop-out rate
            </label>
            <select
              {...register('dropoutRate')}
              className="w-full border border-gray-300 rounded-lg px-2 py-2 text-sm"
            >
              <option value="0.10">10%</option>
              <option value="0.15">15%</option>
              <option value="0.20">20%</option>
              <option value="0.25">25%</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Screen-fail rate
            </label>
            <select
              {...register('screenFailRate')}
              className="w-full border border-gray-300 rounded-lg px-2 py-2 text-sm"
            >
              <option value="0.15">15%</option>
              <option value="0.20">20%</option>
              <option value="0.25">25%</option>
              <option value="0.30">30%</option>
            </select>
          </div>
        </div>
      </div>

      {/* Food Effect */}
      <div className="flex items-center gap-3">
        <input
          {...register('foodEffect')}
          type="checkbox"
          id="foodEffect"
          className="w-4 h-4 rounded border-gray-300 text-blue-600"
        />
        <label htmlFor="foodEffect" className="text-sm font-medium text-gray-700">
          Влияние пищи подтверждено (требуется исследование fed + fasting)
        </label>
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Примечания
        </label>
        <textarea
          {...register('notes')}
          rows={2}
          placeholder="Дополнительные комментарии..."
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold py-3 px-6 rounded-lg transition-colors"
      >
        {loading ? '⏳ Рассчитываем...' : '🔬 Рассчитать дизайн исследования'}
      </button>
    </form>
  )
}
