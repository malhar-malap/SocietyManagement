import React from 'react';
import { Check } from 'lucide-react';

// Mirrors utils/passwordPolicy so people see each rule tick off as they type
const RULES = [
    { label: 'At least 8 characters', test: (p) => p.length >= 8 },
    { label: 'Contains a letter', test: (p) => /[A-Za-z]/.test(p) },
    { label: 'Contains a number', test: (p) => /\d/.test(p) },
];

const PasswordChecklist = ({ password = '' }) => (
    <ul className="checklist" aria-label="Password requirements">
        {RULES.map((r) => {
            const ok = r.test(password);
            return (
                <li key={r.label} className={ok ? 'ok' : ''}>
                    <span className="tick" aria-hidden="true">{ok && <Check size={11} strokeWidth={3} />}</span>
                    {r.label}
                    <span className="visually-hidden">{ok ? '(met)' : '(not met)'}</span>
                </li>
            );
        })}
    </ul>
);

export default PasswordChecklist;
