export type StudyDesign =
  | 'TWO_WAY_CROSSOVER'
  | 'REPLICATE_DESIGN'
  | 'PARALLEL_DESIGN'
  | 'THREE_WAY_CROSSOVER'

export type FoodEffect = 'NONE' | 'FASTING_ONLY' | 'FASTING_AND_FED'

export type RSABEDecision =
  | 'NOT_APPLICABLE'
  | 'APPLICABLE'
  | 'REQUIRED'
  | 'NOT_ALLOWED'

export type RegulationType = 'EEK_85' | 'EMA' | 'FDA'

export interface BECalculationRequest {
  drugName: string
  cvIntra: number
  halfLife: number
  power?: number
  alpha?: number
  dropoutRate?: number
  screenFailRate?: number
  foodEffect?: boolean
  forceRSABE?: boolean | null
  notes?: string
}

export interface PKParameters {
  halfLife: number
  washoutPeriods: number
  washoutHours: number
  washoutDays: number
  accumulationRisk: boolean
  washoutWarning: string | null
}

export interface RSABEResult {
  cvIntra: number
  decision: RSABEDecision
  scalingFactor: number | null
  lowerBound: number
  upperBound: number
  justification: string
}

export interface SampleSizeResult {
  nPerSequence: number
  nRandomized: number
  nScreened: number
  design: StudyDesign
  power: number
  alpha: number
  gmrAssumed: number
  calculation: string
}

export interface StudySynopsis {
  drugName: string
  studyDesign: StudyDesign
  rsabe: RSABEResult
  pkParameters: PKParameters
  sampleSize: SampleSizeResult
  foodEffect: FoodEffect
  regulatoryCompliance: RegulationType[]
  synopsis: string
  markdownExport: string
  jsonExport: Record<string, unknown>
}

export interface BECalculationResponse {
  requestId: string
  drugName: string
  studyDesign: StudyDesign
  rsabe: RSABEResult
  pkParameters: PKParameters
  sampleSize: SampleSizeResult
  foodEffect: FoodEffect
  synopsis: StudySynopsis
  aiExplanation: string | null
  calculatedAt: string
  regulatoryVersion: string
}

export interface RagQueryRequest {
  query: string
  context?: string
  regulation?: RegulationType
}

export interface RagQueryResponse {
  answer: string
  sources: string[]
  confidence: number
}
