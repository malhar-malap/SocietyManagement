import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { MoreHorizontal } from 'lucide-react';

/**
 * A "more actions" menu. It renders in a portal with fixed positioning so it is never clipped by a scrolling
 * table. items: [{ label, icon, onClick, danger, hidden }] or 'separator'.
 */
const Menu = ({ items, label = 'More actions', trigger }) => {
    const [open, setOpen] = useState(false);
    const [pos, setPos] = useState(null);
    const buttonRef = useRef(null);
    const menuRef = useRef(null);
    const visible = items.filter((i) => i && !i.hidden);

    useLayoutEffect(() => {
        if (!open || !buttonRef.current) return;
        const r = buttonRef.current.getBoundingClientRect();
        const menuH = menuRef.current?.offsetHeight || 200;
        const below = r.bottom + 6 + menuH < window.innerHeight;
        setPos({ top: below ? r.bottom + 6 : r.top - 6 - menuH, right: Math.max(8, window.innerWidth - r.right) });
    }, [open]);

    useEffect(() => {
        if (!open) return undefined;
        const close = (e) => {
            if (menuRef.current?.contains(e.target) || buttonRef.current?.contains(e.target)) return;
            setOpen(false);
        };
        const onKey = (e) => { if (e.key === 'Escape') { setOpen(false); buttonRef.current?.focus(); } };
        const onScroll = () => setOpen(false);
        document.addEventListener('mousedown', close);
        document.addEventListener('keydown', onKey);
        window.addEventListener('scroll', onScroll, true);
        window.addEventListener('resize', onScroll);
        menuRef.current?.querySelector('button')?.focus();
        return () => {
            document.removeEventListener('mousedown', close);
            document.removeEventListener('keydown', onKey);
            window.removeEventListener('scroll', onScroll, true);
            window.removeEventListener('resize', onScroll);
        };
    }, [open]);

    if (visible.filter((i) => i !== 'separator').length === 0) return null;

    return (
        <>
            {trigger ? trigger({ ref: buttonRef, onClick: (e) => { e.stopPropagation(); setOpen((o) => !o); }, 'aria-expanded': open }) : (
                <button
                    ref={buttonRef}
                    type="button"
                    className="icon-btn icon-btn-sm"
                    aria-label={label}
                    aria-haspopup="menu"
                    aria-expanded={open}
                    onClick={(e) => { e.stopPropagation(); setOpen((o) => !o); }}
                >
                    <MoreHorizontal size={18} />
                </button>
            )}
            {open && createPortal(
                <div
                    ref={menuRef}
                    className="menu"
                    role="menu"
                    style={{ position: 'fixed', top: pos?.top ?? -9999, right: pos?.right ?? 0 }}
                    onClick={(e) => e.stopPropagation()}
                    onKeyDown={(e) => {
                        if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
                        e.preventDefault();
                        const buttons = [...menuRef.current.querySelectorAll('button')];
                        const i = buttons.indexOf(document.activeElement);
                        buttons[(i + (e.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length]?.focus();
                    }}
                >
                    {visible.map((item, idx) => (item === 'separator' ? <div key={`sep-${idx}`} className="menu-sep" /> : (
                        <button
                            key={item.label}
                            type="button"
                            role="menuitem"
                            className={`menu-item ${item.danger ? 'danger' : ''}`}
                            onClick={() => { setOpen(false); item.onClick(); }}
                        >
                            {item.icon && <item.icon size={16} aria-hidden="true" />} {item.label}
                        </button>
                    )))}
                </div>,
                document.body,
            )}
        </>
    );
};

export default Menu;
