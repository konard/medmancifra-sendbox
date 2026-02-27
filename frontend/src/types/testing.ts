// ─── Testing Module Types ─────────────────────────────────────────────────────

export type ModelTrainingStatus =
  | 'TRAINED'
  | 'TRAINING_IN_PROGRESS'
  | 'NOT_TRAINED'
  | 'FAILED'

export type TestStatus = 'PASSED' | 'FAILED' | 'SKIPPED' | 'IN_PROGRESS'

export type DbMockStatus = 'READY' | 'INITIALIZING' | 'FAILED' | 'NOT_INITIALIZED'

export interface ModelMetrics {
  accuracy: number | null
  rmse: number | null
  mae: number | null
  r2Score: number | null
  trainingDataPoints: number | null
  validationDataPoints: number | null
  trainedAt: string | null
  notes: string | null
}

export interface ModelInfo {
  modelName: string
  modelVersion: string
  status: ModelTrainingStatus
  metrics: ModelMetrics | null
  datasetName: string | null
  datasetUrl: string | null
  datasetDescription: string | null
  canStartTraining: boolean
  trainingMessage: string | null
}

export interface TestScenarioResult {
  scenarioId: string
  scenarioName: string
  description: string
  status: TestStatus
  expectedResult: string
  actualResult: string | null
  errorMessage: string | null
  executionTimeMs: number
  executedAt: string
}

export interface PipelineError {
  stage: string
  errorCode: string
  message: string
  timestamp: string
  isCritical: boolean
  suggestion: string | null
}

export interface DbMockResult {
  status: DbMockStatus
  writeTestPassed: boolean
  readTestPassed: boolean
  recordCount: number
  connectionTimeMs: number
  message: string
}

export interface TestingSummary {
  totalTests: number
  passed: number
  failed: number
  skipped: number
  runAt: string
}

export interface TestingStatusResponse {
  model: ModelInfo
  testResults: TestScenarioResult[]
  pipelineErrors: PipelineError[]
  dbMock: DbMockResult
  summary: TestingSummary
  generatedAt: string
}

export interface TrainingStartResponse {
  started: boolean
  message: string
  estimatedDurationMinutes: number | null
  jobId: string | null
}
