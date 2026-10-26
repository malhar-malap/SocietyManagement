import React from 'react';
import { Receipt, BellRing, ShieldCheck } from 'lucide-react';
import { HibiscusMark } from '../ui/Hibiscus';

const POINTS = [
    { icon: Receipt, title: 'Bills and receipts in one place', text: 'See what is due, pay by UPI or bank transfer, and download receipts any time.' },
    { icon: BellRing, title: 'Never miss a notice', text: 'Water cuts, meetings and events reach every flat the moment they are posted.' },
    { icon: ShieldCheck, title: 'Private and secure', text: 'Your account is verified by email and every admin action is recorded.' },
];

/** Two-column sign-in layout: a calm brand panel beside the form. The panel folds away on phones. */
const AuthShell = ({ children }) => (
    <div className="auth">
        <aside className="auth-brand" aria-hidden="true">
            <div className="auth-brand-top">
                <HibiscusMark size={34} className="on-brand" />
                <span className="auth-brand-name">Jaswanti Residency</span>
            </div>
            <div className="auth-brand-body">
                <h2 className="auth-brand-headline">Everything your home needs, <em>calmly</em> in one place.</h2>
                <ul className="auth-points">
                    {POINTS.map(({ icon: Icon, title, text }) => (
                        <li key={title}>
                            <span className="auth-point-icon"><Icon size={18} /></span>
                            <div>
                                <strong>{title}</strong>
                                <p>{text}</p>
                            </div>
                        </li>
                    ))}
                </ul>
            </div>
            <div className="auth-brand-foot">C Wing · 80 homes · Managed by the society committee</div>
            <HibiscusMark size={520} className="auth-brand-art on-brand" />
        </aside>
        <main className="auth-main">
            <div className="auth-mobile-brand">
                <HibiscusMark size={30} />
                <span>Jaswanti Residency</span>
            </div>
            <div className="auth-panel">{children}</div>
            <p className="auth-legal">Need help signing in? Contact the society office.</p>
        </main>
    </div>
);

export default AuthShell;
