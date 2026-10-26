import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, Inbox } from 'lucide-react';

const ALERT_ICONS = { info: Info, success: CheckCircle2, warning: AlertTriangle, error: AlertCircle, neutral: Info };

export const Alert = ({ tone = 'info', title, children, action, className = '' }) => {
    const Icon = ALERT_ICONS[tone] || Info;
    return (
        <div className={`alert alert-${tone} ${className}`} role={tone === 'error' ? 'alert' : 'status'}>
            <Icon size={17} aria-hidden="true" />
            <div className="grow">
                {title && <div className="alert-title">{title}</div>}
                {children}
            </div>
            {action && <div className="alert-action">{action}</div>}
        </div>
    );
};

export const EmptyState = ({ icon: Icon = Inbox, title, children, action, compact = false }) => (
    <div className="empty-state" style={compact ? { padding: '1.75rem 1rem' } : undefined}>
        <span className="empty-icon"><Icon size={22} aria-hidden="true" /></span>
        {title && <h3>{title}</h3>}
        {children && <p>{children}</p>}
        {action}
    </div>
);

export const Skeleton = ({ width = '100%', height = 14, className = '', style }) => (
    <span className={`skeleton ${className}`} style={{ width, height, ...style }} aria-hidden="true" />
);

export const StatCard = ({ label, value, icon: Icon, tone = 'neutral', foot, onClick, loading = false }) => {
    const interactive = Boolean(onClick);
    return (
        <div
            className={`card stat ${interactive ? 'is-link' : ''}`}
            onClick={onClick}
            onKeyDown={interactive ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onClick(); } } : undefined}
            role={interactive ? 'button' : undefined}
            tabIndex={interactive ? 0 : undefined}
        >
            <div className="stat-top">
                <span className="stat-label">{label}</span>
                {Icon && <span className={`stat-icon tone-${tone}`}><Icon size={17} aria-hidden="true" /></span>}
            </div>
            {loading ? <Skeleton width="60%" height={28} /> : <div className="stat-value">{value}</div>}
            {foot && <div className="stat-foot">{foot}</div>}
        </div>
    );
};

export const Tabs = ({ tabs, value, onChange, label = 'Sections' }) => (
    <div className="tabs" role="tablist" aria-label={label}>
        {tabs.map((t) => (
            <button
                key={t.value}
                type="button"
                role="tab"
                className="tab"
                aria-selected={value === t.value}
                onClick={() => onChange(t.value)}
            >
                {t.icon && <t.icon size={16} aria-hidden="true" />}
                {t.label}
                {t.count != null && <span className="tab-count">{t.count}</span>}
            </button>
        ))}
    </div>
);

export const Segmented = ({ options, value, onChange, label }) => (
    <div className="segmented" role="group" aria-label={label}>
        {options.map((o) => (
            <button key={o.value} type="button" aria-pressed={value === o.value} onClick={() => onChange(o.value)}>{o.label}</button>
        ))}
    </div>
);
