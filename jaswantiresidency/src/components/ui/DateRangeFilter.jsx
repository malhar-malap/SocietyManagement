import React, { useId } from 'react';
import { CalendarRange } from 'lucide-react';
import { DATE_PRESETS, rangeForPreset } from '../../utils/dateRange';

/** Compact date filter for list toolbars: a preset picker, plus from/to inputs when "Custom range" is chosen. */
const DateRangeFilter = ({ value, onChange, label = 'Filter by date' }) => {
    const id = `dr${useId().replace(/:/g, '')}`;
    const choosePreset = (preset) => {
        if (preset === 'custom') onChange({ preset, from: value.from, to: value.to });
        else onChange({ preset, ...rangeForPreset(preset) });
    };
    return (
        <div className="date-filter" role="group" aria-label={label}>
            <div className="date-filter-select">
                <CalendarRange size={16} aria-hidden="true" />
                <select id={id} aria-label={label} value={value.preset} onChange={(e) => choosePreset(e.target.value)}>
                    {DATE_PRESETS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
                </select>
            </div>
            {value.preset === 'custom' && (
                <>
                    <input type="date" aria-label="From date" value={value.from} max={value.to || undefined}
                        onChange={(e) => onChange({ ...value, from: e.target.value })} />
                    <span className="text-subtle text-xs" aria-hidden="true">to</span>
                    <input type="date" aria-label="To date" value={value.to} min={value.from || undefined}
                        onChange={(e) => onChange({ ...value, to: e.target.value })} />
                </>
            )}
        </div>
    );
};

export default DateRangeFilter;
