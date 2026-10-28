import { inDateRange, rangeForPreset } from './dateRange';

describe('date range filter', () => {
    const oct27 = new Date(2026, 9, 27);

    test('presets cover whole calendar months', () => {
        expect(rangeForPreset('this-month', oct27)).toEqual({ from: '2026-10-01', to: '2026-10-31' });
        expect(rangeForPreset('last-month', oct27)).toEqual({ from: '2026-09-01', to: '2026-09-30' });
        expect(rangeForPreset('last-3-months', oct27)).toEqual({ from: '2026-08-01', to: '2026-10-31' });
        expect(rangeForPreset('last-month', new Date(2026, 0, 15))).toEqual({ from: '2025-12-01', to: '2025-12-31' });
        expect(rangeForPreset('all', oct27)).toEqual({ from: '', to: '' });
    });

    test('both ends are inclusive, for plain dates and timestamps', () => {
        const range = { from: '2026-10-01', to: '2026-10-31' };
        expect(inDateRange('2026-10-01', range)).toBe(true);
        expect(inDateRange('2026-10-31', range)).toBe(true);
        expect(inDateRange('2026-10-31T23:30:00', range)).toBe(true);
        expect(inDateRange('2026-11-01', range)).toBe(false);
        expect(inDateRange('2026-09-30T10:00:00', range)).toBe(false);
    });

    test('open-ended and empty ranges', () => {
        expect(inDateRange('2020-01-01', { from: '', to: '' })).toBe(true);
        expect(inDateRange(null, { from: '', to: '' })).toBe(true);
        expect(inDateRange(null, { from: '2026-10-01', to: '' })).toBe(false);
        expect(inDateRange('2027-05-01', { from: '2026-10-01', to: '' })).toBe(true);
        expect(inDateRange('2026-05-01', { from: '', to: '2026-06-01' })).toBe(true);
    });
});
