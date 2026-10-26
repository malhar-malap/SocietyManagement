// Shared display formatting so money, dates and names read the same on every page

export const formatMoney = (value, fractionDigits = 0) =>
    `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits })}`;

/** ₹1.2L / ₹45k style, for chart axes and tight spaces. */
export const formatMoneyShort = (value) => {
    const n = Number(value || 0);
    if (Math.abs(n) >= 100000) return `₹${(n / 100000).toFixed(n % 100000 === 0 ? 0 : 1)}L`;
    if (Math.abs(n) >= 1000) return `₹${Math.round(n / 1000)}k`;
    return `₹${n}`;
};

export const formatDate = (value, opts = { day: 'numeric', month: 'short', year: 'numeric' }) => {
    if (!value) return '—';
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString('en-IN', opts);
};

export const formatDateTime = (value) => {
    if (!value) return '—';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return `${d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}, ${d.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })}`;
};

/** "2026-10" -> "October 2026" */
export const monthLabel = (ym, month = 'long') => {
    if (!ym) return '';
    const [y, m] = String(ym).split('-').map(Number);
    if (!y || !m) return String(ym);
    return new Date(y, m - 1, 1).toLocaleDateString('en-IN', { month, year: 'numeric' });
};

export const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
};

export const firstName = (name) => (name || '').trim().split(/\s+/)[0] || '';

export const timeAgo = (iso) => {
    if (!iso) return '';
    const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours} h ago`;
    const days = Math.round(hours / 24);
    if (days < 7) return `${days} d ago`;
    return formatDate(iso, { day: 'numeric', month: 'short' });
};
