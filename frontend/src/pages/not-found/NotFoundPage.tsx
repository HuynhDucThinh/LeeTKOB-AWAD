import { ArrowLeft, SearchX } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export function NotFoundPage() {
  const navigate = useNavigate()
  return <div className="grid min-h-[70vh] place-items-center"><div className="text-center"><span className="mx-auto grid size-16 place-items-center rounded-3xl bg-lime-50 text-lime-600"><SearchX className="size-7" /></span><p className="mt-6 font-mono text-sm text-lime-600">404</p><h1 className="mt-2 text-3xl font-semibold text-slate-950">Page not found</h1><p className="mt-2 text-sm text-slate-500">This resource does not exist or is outside your assigned class.</p><button onClick={() => navigate(-1)} className="primary-button mx-auto mt-6"><ArrowLeft className="size-4" /> Go back</button></div></div>
}
