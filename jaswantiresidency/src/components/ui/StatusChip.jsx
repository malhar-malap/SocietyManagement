import React from 'react';

const TONES = {
    success: ['paid', 'paid_cash', 'active', 'resolved', 'approved', 'completed', 'closed', 'verified', 'success', 'credit', 'occupied'],
    warning: ['pending', 'open', 'in progress', 'in_progress', 'partially paid', 'partially_paid', 'awaiting approval', 'submitted', 'pending_approval'],
    danger: ['blocked', 'deactivated', 'inactive', 'overdue', 'rejected', 'failed', 'denied', 'debit', 'cancelled'],
    info: ['paid (cash)', 'acknowledged', 'query', 'info', 'vacant'],
};

const toneFor = (status) => {
    const s = String(status ?? '').toLowerCase().trim();
    return Object.keys(TONES).find((tone) => TONES[tone].includes(s)) || 'neutral';
};

/** "PARTIALLY_PAID" -> "Partially paid" */
export const humanize = (value) => {
    if (value == null || value === '') return '';
    const s = String(value).replace(/_/g, ' ').toLowerCase();
    return s.charAt(0).toUpperCase() + s.slice(1);
};

const StatusChip = ({ status, tone, label, className = '' }) => (
    <span className={`chip chip-${tone || toneFor(status)} ${className}`}>{label || humanize(status)}</span>
);

export default StatusChip;
