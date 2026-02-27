# Datasets — BE Platform

Синтетические и реальные (из открытых публикаций) наборы данных для обучения RAG-модели.

## Файлы

### `pk_reference_data.json`
ФК-параметры референтных препаратов (онкология, открытые публикации):
- 12 препаратов: иматиниб, сорафениб, сунитиниб, эрлотиниб, нилотиниб, лапатиниб, дасатиниб, эверолимус, капецитабин, темозоломид, кабозантиниб, палбоциклиб
- Поля: CV_intra, CV_between, t½, Tmax, биодоступность, AUC, Cmax, связывание с белками, food effect, рекомендуемый дизайн, PubMed ID

### `regulatory_qa_rag.json`
База знаний вопрос-ответ для RAG (GigaChat):
- 12 Q&A пар по регуляторным вопросам
- Темы: RSABE, дизайн, washout, критерии 80-125%, расчёт выборки, food effect
- Источники: ЕЭК №85, EMA, FDA Guidance

### `sample_synopses.json`
Готовые синопсисы исследований для примеров и тестирования:
- 4 синопсиса разного уровня сложности
- Формат: входные параметры + выходные данные + текст синопсиса

## Использование в RAG

Загрузка в GigaChat RAG:
```python
# Пример загрузки через Langchain
from langchain.document_loaders import JSONLoader
from langchain.vectorstores import Chroma

loader = JSONLoader(file_path="pk_reference_data.json", jq_schema=".[]", text_content=False)
docs = loader.load()
vectorstore = Chroma.from_documents(docs, embedding_function)
```

## Источники данных

Все ФК-данные взяты из открытых публикаций PubMed. PubMed ID указан в каждом записи.
