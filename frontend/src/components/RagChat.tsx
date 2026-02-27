import { useState, useRef, useEffect } from 'react'
import { ragQuery } from '../utils/api'
import type { RagQueryResponse } from '../types/api'

interface Message {
  role: 'user' | 'assistant'
  content: string
  sources?: string[]
}

const SAMPLE_QUESTIONS = [
  'Когда применяется RSABE?',
  'Каковы критерии 80-125% по ЕЭК №85?',
  'Чем отличается EMA от FDA в требованиях к БЭ?',
  'Что такое washout период?',
  'Когда нужен параллельный дизайн?',
]

export default function RagChat() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: 'Здравствуйте! Я AI-ассистент по регуляторным требованиям к исследованиям биоэквивалентности. Задайте вопрос по ЕЭК №85, EMA или FDA.',
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const sendMessage = async (text: string) => {
    if (!text.trim() || loading) return

    const userMsg: Message = { role: 'user', content: text }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      const response: RagQueryResponse = await ragQuery({ query: text })
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
      }])
    } catch (err) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Ошибка при обращении к AI-ассистенту. Убедитесь, что backend запущен.',
      }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-[600px] bg-white border rounded-xl overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-indigo-800 text-white px-4 py-3">
        <h2 className="font-semibold">🤖 AI-ассистент (RAG / GigaChat)</h2>
        <p className="text-indigo-200 text-xs">Регуляторная база знаний: ЕЭК №85 · EMA · FDA</p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm ${
              msg.role === 'user'
                ? 'bg-indigo-600 text-white rounded-br-md'
                : 'bg-gray-100 text-gray-800 rounded-bl-md'
            }`}>
              <p>{msg.content}</p>
              {msg.sources && msg.sources.length > 0 && (
                <div className="mt-2 pt-2 border-t border-gray-200">
                  <p className="text-xs text-gray-500 font-medium">Источники:</p>
                  {msg.sources.map((s, j) => (
                    <p key={j} className="text-xs text-indigo-600">• {s}</p>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-gray-100 rounded-2xl rounded-bl-md px-4 py-2.5">
              <span className="text-gray-400 text-sm">⏳ GigaChat думает...</span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Sample questions */}
      <div className="px-4 pb-2">
        <div className="flex flex-wrap gap-1">
          {SAMPLE_QUESTIONS.map((q, i) => (
            <button
              key={i}
              onClick={() => sendMessage(q)}
              disabled={loading}
              className="text-xs bg-gray-100 hover:bg-gray-200 text-gray-600 px-2 py-1 rounded-full border transition-colors"
            >
              {q}
            </button>
          ))}
        </div>
      </div>

      {/* Input */}
      <div className="border-t p-3 flex gap-2">
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !e.shiftKey && sendMessage(input)}
          placeholder="Задайте вопрос о регуляторных требованиях..."
          className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500"
        />
        <button
          onClick={() => sendMessage(input)}
          disabled={loading || !input.trim()}
          className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white px-4 py-2 rounded-lg text-sm font-medium"
        >
          ➤
        </button>
      </div>
    </div>
  )
}
