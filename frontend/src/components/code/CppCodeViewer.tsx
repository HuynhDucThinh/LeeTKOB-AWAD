import { useState } from 'react'
import type { EvidenceLine } from '@/types/review'
import { cn } from '@/lib/utils'

export function CppCodeViewer({ sourceCode, strippedCode, evidence }: { sourceCode: string; strippedCode: string; evidence: EvidenceLine[] }) {
  const [stripped, setStripped] = useState(false)
  const lines = (stripped ? strippedCode : sourceCode).split('\n')
  const evidenceLines = new Set(evidence.map((item) => item.line))

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-800 bg-[#0e120c] text-slate-200 shadow-xl shadow-lime-950/10">
      <div className="flex items-center justify-between border-b border-slate-700/70 px-4 py-3">
        <span className="font-mono text-xs text-slate-400">submission.cpp</span>
        <div className="flex rounded-lg bg-slate-800 p-0.5 text-[10px]">
          <button onClick={() => setStripped(false)} className={cn('rounded-md px-2 py-1', !stripped && 'bg-lime-500 text-white')}>Original</button>
          <button onClick={() => setStripped(true)} className={cn('rounded-md px-2 py-1', stripped && 'bg-lime-500 text-white')}>Stripped</button>
        </div>
      </div>
      <div className="max-h-[420px] overflow-auto py-3 font-mono text-[12px] leading-6">
        {lines.map((line, index) => {
          const lineNumber = index + 1
          const active = evidenceLines.has(lineNumber)
          return (
            <div key={lineNumber} id={`line-${lineNumber}`} className={cn('grid grid-cols-[48px_1fr] px-3', active && 'bg-amber-400/15 text-amber-100')}>
              <span className={cn('select-none border-r border-slate-700 pr-3 text-right text-slate-600', active && 'text-amber-400')}>{lineNumber}</span>
              <code className="whitespace-pre pl-4">{line || ' '}</code>
            </div>
          )
        })}
      </div>
    </div>
  )
}
