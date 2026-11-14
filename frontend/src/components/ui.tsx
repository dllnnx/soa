import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode } from 'react';
import { AlertCircle, Check, Clock3, Trash2, X } from 'lucide-react';
import type { Toast } from '../types';
import { NUMBER_MAX_LENGTH } from '../validation';

export function TooltipButton({ label, tooltip, className = '', disabled, onClick }: { label: string; tooltip: string; className?: string; disabled?: boolean; onClick: () => void }) {
  const tooltipId = useId();
  return <span className="action-tooltip-wrap">
    <button className={className} disabled={disabled} onClick={onClick} aria-describedby={tooltipId}>{label}</button>
    <span className="action-tooltip" id={tooltipId} role="tooltip">{tooltip}</span>
  </span>;
}

export function ToastCard({ toast, onClose }: { toast: NonNullable<Toast>; onClose: () => void }) {
  return <div className={`toast ${toast.kind}`} role="status">
    <span>{toast.kind === 'success' ? <Check /> : toast.kind === 'error' ? <AlertCircle /> : <Clock3 />}</span>
    <div><b>{toast.title}</b><p>{toast.message}</p></div>
    <button onClick={onClose}><X size={16} /></button>
  </div>;
}

export function ConfirmModal({ title, text, confirm, onCancel, onConfirm }: { title: string; text: string; confirm: string; onCancel: () => void; onConfirm: () => void }) {
  return <div className="overlay confirm-overlay"><div className="modal confirm-modal"><span className="confirm-icon"><Trash2 /></span><h2>{title}</h2><p>{text}</p><div><button className="button secondary" onClick={onCancel}>Отмена</button><button className="button danger" onClick={onConfirm}>{confirm}</button></div></div></div>;
}

export function FilterSection({ title, children }: { title: string; children: ReactNode }) { return <section className="filter-section"><h3>{title}</h3><div className="filter-grid">{children}</div></section>; }

export function Field({ label, hint, error, wide, children }: { label: string; hint?: string; error?: string; wide?: boolean; children: ReactNode }) { return <label className={`field ${wide ? 'wide' : ''} ${error ? 'invalid' : ''}`}><span>{label}{hint && <small>{hint}</small>}</span>{children}{error && <em>{error}</em>}</label>; }

export function NumericInput({ integer, ...props }: Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'maxLength' | 'inputMode'> & { integer?: boolean }) {
  return <input {...props} type="text" inputMode={integer ? 'numeric' : 'decimal'} maxLength={NUMBER_MAX_LENGTH} autoComplete="off" spellCheck={false} />;
}

export function RangeFields({ label, from, to, onFrom, onTo, integer, error }: { label: string; from: string; to: string; onFrom: (value: string) => void; onTo: (value: string) => void; integer?: boolean; error?: string }) {
  return <Field label={label} wide error={error}><div className="range-fields"><NumericInput integer={integer} value={from} onChange={(e) => onFrom(e.target.value)} placeholder="Строго больше" /><span>—</span><NumericInput integer={integer} value={to} onChange={(e) => onTo(e.target.value)} placeholder="Строго меньше" /></div></Field>;
}
