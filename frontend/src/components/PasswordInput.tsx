import { useState, type ChangeEventHandler, type KeyboardEventHandler } from "react";

interface PasswordInputProps {
  id: string;
  label?: string;
  value: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
  onKeyDown?: KeyboardEventHandler<HTMLInputElement>;
  placeholder?: string;
  autoComplete?: string;
}

export default function PasswordInput({
  id,
  label,
  value,
  onChange,
  onKeyDown,
  placeholder,
  autoComplete,
}: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div>
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-slate-300 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          id={id}
          type={visible ? "text" : "password"}
          value={value}
          onChange={onChange}
          onKeyDown={onKeyDown}
          className="w-full rounded-lg bg-slate-800 border border-slate-700 px-3.5 py-2.5 pr-14 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
          placeholder={placeholder}
          autoComplete={autoComplete}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          tabIndex={-1}
          className="absolute inset-y-0 right-0 px-3.5 text-xs font-medium text-slate-400 hover:text-slate-200 transition"
        >
          {visible ? "Hide" : "Show"}
        </button>
      </div>
    </div>
  );
}
