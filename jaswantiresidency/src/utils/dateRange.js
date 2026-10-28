// Date-range presets for admin list filters. Ranges are inclusive "YYYY-MM-DD" strings; empty means open-ended.
const pad = (n) => String(n).padStart(2, '0');
export const isoDay = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export const DATE_PRESETS = [
    { value: 'all', label: 'All dates' },
    { value: 'this-month', label: 'This month' },
    { value: 'last-month', label: 'Last month' },
    { value: 'last-3-months', label: 'Last 3 months' },
    { value: 'this-year', label: 'This year' },
    { value: 'custom', label: 'Custom range' },
];

export const ALL_DATES = { preset: 'all', from: '', to: '' };

export const rangeForPreset = (preset, today = new Date()) => {
    const y = today.getFullYear();
    const m = today.getMonth();
    switch (preset) {
        case 'this-month': return { from: isoDay(new Date(y, m, 1)), to: isoDay(new Date(y, m + 1, 0)) };
        case 'last-month': return { from: isoDay(new Date(y, m - 1, 1)), to: isoDay(new Date(y, m, 0)) };
        case 'last-3-months': return { from: isoDay(new Date(y, m - 2, 1)), to: isoDay(new Date(y, m + 1, 0)) };
        case 'this-year': return { from: `${y}-01-01`, to: `${y}-12-31` };
        default: return { from: '', to: '' };
    }
};

export const isRangeActive = (range) => Boolean(range && (range.from || range.to));

/** True when the value's calendar day falls inside the range. Values without a date only pass an empty range. */
export const inDateRange = (value, range) => {
    if (!isRangeActive(range)) return true;
    if (!value) return false;
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return false;
    // Plain "YYYY-MM-DD" values are already a calendar day; timestamps are compared by their local day
    const day = /^\d{4}-\d{2}-\d{2}$/.test(String(value)) ? String(value) : isoDay(d);
    return (!range.from || day >= range.from) && (!range.to || day <= range.to);
};
