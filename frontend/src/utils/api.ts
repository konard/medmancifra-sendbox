import axios from 'axios'
import type {
  BECalculationRequest,
  BECalculationResponse,
  RagQueryRequest,
  RagQueryResponse,
} from '../types/api'
import type { TestingStatusResponse, TrainingStartResponse } from '../types/testing'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

export const calculate = (req: BECalculationRequest): Promise<BECalculationResponse> =>
  api.post<BECalculationResponse>('/calculate', req).then(r => r.data)

export const ragQuery = (req: RagQueryRequest): Promise<RagQueryResponse> =>
  api.post<RagQueryResponse>('/rag/query', req).then(r => r.data)

export const fetchDesigns = () =>
  api.get('/designs').then(r => r.data)

export const healthCheck = () =>
  api.get('/health').then(r => r.data)

export const getTestingStatus = (): Promise<TestingStatusResponse> =>
  api.get<TestingStatusResponse>('/testing/status').then(r => r.data)

export const startModelTraining = (): Promise<TrainingStartResponse> =>
  api.post<TrainingStartResponse>('/testing/train').then(r => r.data)
