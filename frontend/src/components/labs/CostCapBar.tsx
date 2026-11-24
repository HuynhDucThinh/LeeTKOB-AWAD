export function CostCapBar({ spent, cap }: { spent: number; cap: number }) {
  const percentage = Math.min(100, Math.round((spent / cap) * 100))
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-xs"><span className="text-slate-500">Budget usage</span><span className="font-semibold text-slate-800">${spent.toFixed(2)} / ${cap.toFixed(2)}</span></div>
      <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-gradient-to-r from-lime-500 to-emerald-400" style={{ width: `${percentage}%` }} /></div>
    </div>
  )
}
