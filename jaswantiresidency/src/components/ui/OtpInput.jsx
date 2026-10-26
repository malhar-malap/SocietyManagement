import React, { useEffect, useRef } from 'react';

/** Six single-digit boxes: typing advances, Backspace goes back, and pasting a whole code fills every box. */
const OtpInput = ({ value, onChange, length = 6, invalid = false, autoFocus = false, disabled = false, label = 'Verification code' }) => {
    const refs = useRef([]);
    // After a wrong code the boxes are cleared; put the cursor back at the start so the next try is one motion
    useEffect(() => {
        if (!value && invalid) refs.current[0]?.focus();
    }, [value, invalid]);

    const digits = Array.from({ length }, (_, i) => value[i] || '');

    const setAt = (index, char) => {
        const next = digits.slice();
        next[index] = char;
        onChange(next.join('').slice(0, length));
    };

    const fill = (text, start = 0) => {
        const clean = text.replace(/\D/g, '').slice(0, length - start);
        if (!clean) return;
        const next = digits.slice();
        clean.split('').forEach((c, i) => { next[start + i] = c; });
        onChange(next.join(''));
        refs.current[Math.min(start + clean.length, length - 1)]?.focus();
    };

    return (
        <div className={`otp ${invalid ? 'is-invalid' : ''}`} role="group" aria-label={label}>
            {digits.map((d, i) => (
                <input
                    key={i}
                    ref={(el) => { refs.current[i] = el; }}
                    value={d}
                    inputMode="numeric"
                    autoComplete={i === 0 ? 'one-time-code' : 'off'}
                    aria-label={`Digit ${i + 1} of ${length}`}
                    maxLength={length}
                    autoFocus={autoFocus && i === 0}
                    disabled={disabled}
                    onFocus={(e) => e.target.select()}
                    onChange={(e) => {
                        const v = e.target.value.replace(/\D/g, '');
                        if (v.length > 1) { fill(v, i); return; }
                        setAt(i, v);
                        if (v && i < length - 1) refs.current[i + 1]?.focus();
                    }}
                    onKeyDown={(e) => {
                        if (e.key === 'Backspace' && !digits[i] && i > 0) { refs.current[i - 1]?.focus(); setAt(i - 1, ''); e.preventDefault(); }
                        if (e.key === 'ArrowLeft' && i > 0) refs.current[i - 1]?.focus();
                        if (e.key === 'ArrowRight' && i < length - 1) refs.current[i + 1]?.focus();
                    }}
                    onPaste={(e) => { e.preventDefault(); fill(e.clipboardData.getData('text'), i); }}
                />
            ))}
        </div>
    );
};

export default OtpInput;
