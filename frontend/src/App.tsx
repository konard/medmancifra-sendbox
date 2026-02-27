import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Calculator from './pages/Calculator'
import AiChat from './pages/AiChat'
import Testing from './pages/Testing'

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-sm">
              BE
            </div>
            <div>
              <h1 className="font-bold text-gray-900 leading-none text-sm">BE Platform</h1>
              <p className="text-xs text-gray-400">Автоматизация БЭ исследований</p>
            </div>
          </div>
          <nav className="flex gap-1">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              🔬 Калькулятор
            </NavLink>
            <NavLink
              to="/chat"
              className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-indigo-600 text-white' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              🤖 AI-чат
            </NavLink>
            <NavLink
              to="/testing"
              className={({ isActive }) =>
                `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-emerald-600 text-white' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              🧪 Тестирование
            </NavLink>
          </nav>
        </div>
      </header>

      {/* Banner */}
      <div className="bg-gradient-to-r from-blue-700 to-indigo-700 text-white">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-3">
          <span className="text-2xl">🚀</span>
          <div>
            <p className="font-semibold text-sm">
              AI-ПЛАТФОРМА АВТОМАТИЗАЦИИ ИССЛЕДОВАНИЙ БИОЭКВИВАЛЕНТНОСТИ (ОНКОЛОГИЯ)
            </p>
            <p className="text-blue-200 text-xs">
              Детерминированное расчётное ядро · RAG на GigaChat · ЕЭК №85 · EMA · FDA
            </p>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-6">
        {children}
      </main>

      {/* Footer */}
      <footer className="border-t bg-white mt-8">
        <div className="max-w-7xl mx-auto px-4 py-4 text-center text-xs text-gray-400">
          BE Platform v1.0.0 · Kotlin + Quarkus · React + TypeScript · GigaChat RAG
        </div>
      </footer>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Calculator />} />
          <Route path="/chat" element={<AiChat />} />
          <Route path="/testing" element={<Testing />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
