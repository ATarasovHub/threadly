import { useId } from 'react';

interface FieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: 'text' | 'password' | 'email';
  autoComplete?: string;
  required?: boolean;
  /** Validation message from the API for this field. */
  error?: string;
  hint?: string;
}

export function Field({
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
  required,
  error,
  hint,
}: FieldProps) {
  const id = useId();
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-ink-muted">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        required={required}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)}
        className={`mt-1 w-full rounded-lg border bg-surface px-3 py-2 text-ink outline-none transition focus:border-brand ${
          error ? 'border-like' : 'border-line'
        }`}
      />
      {error ? (
        <p id={`${id}-error`} className="mt-1 text-sm text-like">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="mt-1 text-sm text-ink-muted">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
