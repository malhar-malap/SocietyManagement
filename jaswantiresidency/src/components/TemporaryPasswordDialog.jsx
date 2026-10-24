import React, { useState } from 'react';
import { KeyRound, Copy, Check } from 'lucide-react';

/**
 * Shows a newly issued temporary password exactly once. It is not stored anywhere in the app afterwards, so the
 * admin must pass it on now; the user is forced to choose their own password at first sign-in.
 */
const TemporaryPasswordDialog = ({ info, onClose }) => {
    const [copied, setCopied] = useState(false);
    if (!info) return null;

    const copy = async () => {
        try {
            await navigator.clipboard.writeText(info.password);
            setCopied(true);
        } catch {
            setCopied(false);
        }
    };

    return (
        <div className="premium-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="temp-password-title">
            <div className="premium-modal-content" style={{ maxWidth: '440px' }}>
                <div className="premium-modal-header">
                    <h3 id="temp-password-title" className="premium-modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <KeyRound size={20} aria-hidden="true" /> Temporary password
                    </h3>
                </div>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary-light)', margin: '0.5rem 0 1rem' }}>
                    Share this with <strong>{info.name}</strong> privately. It is shown only once, and they will be asked to
                    set their own password when they first sign in.
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--background-light)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', padding: '0.75rem 1rem' }}>
                    <code style={{ flex: 1, fontSize: '1.15rem', letterSpacing: '0.08em' }}>{info.password}</code>
                    <button type="button" className="custom-btn btn-secondary" onClick={copy} aria-label="Copy temporary password">
                        {copied ? <Check size={16} /> : <Copy size={16} />} {copied ? 'Copied' : 'Copy'}
                    </button>
                </div>
                <div className="premium-actions" style={{ marginTop: '1.25rem' }}>
                    <button type="button" className="custom-btn primary" onClick={onClose}>Done</button>
                </div>
            </div>
        </div>
    );
};

export default TemporaryPasswordDialog;
