import RagChat from '../components/RagChat'

export default function AiChat() {
  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          🤖 AI-ассистент
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          Регуляторная база знаний: ЕЭК №85 · EMA CPMP/QWP/EWP/1401/98 Rev.1 · FDA Guidance 2003
        </p>
      </div>
      <RagChat />
      <div className="mt-4 bg-blue-50 border border-blue-200 rounded-xl p-4">
        <h3 className="text-sm font-semibold text-blue-800 mb-2">ℹ️ О AI-ассистенте</h3>
        <ul className="text-xs text-blue-700 space-y-1">
          <li>• Работает на GigaChat (Сбербанк) с RAG-архитектурой</li>
          <li>• Отвечает строго на основе регуляторных документов</li>
          <li>• <strong>Не участвует в математических расчётах</strong> (только объяснения)</li>
          <li>• Для подключения AI укажите GIGACHAT_CLIENT_ID и GIGACHAT_CLIENT_SECRET</li>
        </ul>
      </div>
    </div>
  )
}
