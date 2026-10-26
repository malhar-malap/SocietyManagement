import React, { useId, useState } from 'react';
import { AlertCircle, ChevronDown, Eye, EyeOff, Paperclip, UploadCloud } from 'lucide-react';

/** Label + control + hint/error, wired together for screen readers. */
export const Field = ({ label, hint, error, required, optional, htmlFor, className = '', children }) => (
    <div className={`field ${className}`}>
        {label && (
            <label className="field-label" htmlFor={htmlFor}>
                {label}
                {required && <span className="required" aria-hidden="true">*</span>}
                {optional && <span className="optional">(optional)</span>}
            </label>
        )}
        {children}
        {error ? (
            <span className="field-error" id={htmlFor ? `${htmlFor}-msg` : undefined} role="alert">
                <AlertCircle size={13} aria-hidden="true" /> {error}
            </span>
        ) : hint ? (
            <span className="field-hint" id={htmlFor ? `${htmlFor}-msg` : undefined}>{hint}</span>
        ) : null}
    </div>
);

const useFieldId = (id) => {
    const generated = useId();
    return id || `f${generated.replace(/:/g, '')}`;
};

export const TextInput = React.forwardRef(({
    label, hint, error, required, optional, icon: Icon, prefix, id, className = '', type = 'text', disabled, ...props
}, ref) => {
    const fieldId = useFieldId(id);
    const [reveal, setReveal] = useState(false);
    const isPassword = type === 'password';
    return (
        <Field label={label} hint={hint} error={error} required={required} optional={optional} htmlFor={fieldId} className={className}>
            <div className={`control ${error ? 'is-invalid' : ''} ${disabled ? 'is-disabled' : ''}`}>
                {Icon && <Icon size={17} className="control-icon" aria-hidden="true" />}
                {prefix && <span className="control-prefix">{prefix}</span>}
                <input
                    ref={ref}
                    id={fieldId}
                    type={isPassword && reveal ? 'text' : type}
                    required={required}
                    disabled={disabled}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={error || hint ? `${fieldId}-msg` : undefined}
                    {...props}
                />
                {isPassword && (
                    <button type="button" className="control-action" onClick={() => setReveal((r) => !r)}
                        aria-label={reveal ? 'Hide password' : 'Show password'} aria-pressed={reveal}>
                        {reveal ? <EyeOff size={17} /> : <Eye size={17} />}
                    </button>
                )}
            </div>
        </Field>
    );
});
TextInput.displayName = 'TextInput';

export const Select = React.forwardRef(({ label, hint, error, required, optional, icon: Icon, id, className = '', children, disabled, ...props }, ref) => {
    const fieldId = useFieldId(id);
    return (
        <Field label={label} hint={hint} error={error} required={required} optional={optional} htmlFor={fieldId} className={className}>
            <div className={`control ${error ? 'is-invalid' : ''} ${disabled ? 'is-disabled' : ''}`}>
                {Icon && <Icon size={17} className="control-icon" aria-hidden="true" />}
                <select ref={ref} id={fieldId} required={required} disabled={disabled} aria-invalid={error ? true : undefined}
                    aria-describedby={error || hint ? `${fieldId}-msg` : undefined} {...props}>
                    {children}
                </select>
                <ChevronDown size={16} className="control-chevron" aria-hidden="true" />
            </div>
        </Field>
    );
});
Select.displayName = 'Select';

export const Textarea = React.forwardRef(({ label, hint, error, required, optional, id, className = '', ...props }, ref) => {
    const fieldId = useFieldId(id);
    return (
        <Field label={label} hint={hint} error={error} required={required} optional={optional} htmlFor={fieldId} className={className}>
            <div className={`control ${error ? 'is-invalid' : ''}`}>
                <textarea ref={ref} id={fieldId} required={required} aria-invalid={error ? true : undefined}
                    aria-describedby={error || hint ? `${fieldId}-msg` : undefined} {...props} />
            </div>
        </Field>
    );
});
Textarea.displayName = 'Textarea';

export default TextInput;

/** A calm file picker: a dashed drop area that shows the chosen file and lets you clear it. */
export const FileInput = ({ label, hint, accept, file, onChange, optional, id, error }) => {
    const fieldId = useFieldId(id);
    const [dragging, setDragging] = useState(false);
    return (
        <Field label={label} hint={hint} optional={optional} htmlFor={fieldId} error={error}>
            <label
                htmlFor={fieldId}
                className={`dropzone ${dragging ? 'is-active' : ''}`}
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={(e) => { e.preventDefault(); setDragging(false); if (e.dataTransfer.files?.[0]) onChange(e.dataTransfer.files[0]); }}
            >
                {file ? (
                    <span className="row" style={{ justifyContent: 'center' }}>
                        <Paperclip size={15} aria-hidden="true" />
                        <span className="fw-600 truncate" style={{ maxWidth: 260, color: 'var(--text)' }}>{file.name}</span>
                        <button type="button" className="btn btn-link btn-sm" onClick={(e) => { e.preventDefault(); onChange(null); }}>Remove</button>
                    </span>
                ) : (
                    <span><UploadCloud size={20} aria-hidden="true" style={{ display: 'block', margin: '0 auto 0.35rem' }} /><strong style={{ color: 'var(--brand)' }}>Choose a file</strong> or drag it here</span>
                )}
                <input id={fieldId} type="file" accept={accept} className="visually-hidden" onChange={(e) => onChange(e.target.files?.[0] || null)} />
            </label>
        </Field>
    );
};
