import React, { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Accessible dialog: Escape and the backdrop close it (unless `dismissible` is false, e.g. while saving),
 * focus moves inside and is trapped, the page behind stops scrolling, and focus returns on close.
 * On phones it becomes a bottom sheet.
 */
const Modal = ({
    isOpen = true, onClose, title, description, icon: Icon, tone = 'brand', size = 'md',
    footer, children, dismissible = true, initialFocus, as: As = 'div', onSubmit, className = '',
}) => {
    const dialogRef = useRef(null);
    const titleId = `modal-${useId().replace(/:/g, '')}`;
    const onCloseRef = useRef(onClose);
    onCloseRef.current = onClose;
    const dismissibleRef = useRef(dismissible);
    dismissibleRef.current = dismissible;

    useEffect(() => {
        if (!isOpen) return undefined;
        const previouslyFocused = document.activeElement;
        const { overflow } = document.body.style;
        document.body.style.overflow = 'hidden';

        const dialog = dialogRef.current;
        const target = (initialFocus && dialog?.querySelector(initialFocus))
            || dialog?.querySelector('[data-autofocus]')
            || dialog?.querySelector('.modal-body ' + FOCUSABLE.split(', ').join(', .modal-body '))
            || dialog;
        target?.focus?.();

        const onKey = (e) => {
            if (e.key === 'Escape' && dismissibleRef.current && onCloseRef.current) {
                e.stopPropagation();
                onCloseRef.current();
            } else if (e.key === 'Tab' && dialog) {
                const items = [...dialog.querySelectorAll(FOCUSABLE)].filter((el) => el.offsetParent !== null);
                if (!items.length) return;
                const first = items[0];
                const last = items[items.length - 1];
                if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
                else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
            }
        };
        document.addEventListener('keydown', onKey);
        return () => {
            document.removeEventListener('keydown', onKey);
            document.body.style.overflow = overflow;
            previouslyFocused?.focus?.();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen]);

    if (!isOpen) return null;

    const onBackdrop = (e) => {
        if (e.target === e.currentTarget && dismissible && onClose) onClose();
    };

    return createPortal(
        <div className="modal-backdrop" onMouseDown={onBackdrop}>
            <As
                ref={dialogRef}
                className={`modal modal-${size} ${className}`}
                role="dialog"
                aria-modal="true"
                aria-labelledby={title ? titleId : undefined}
                tabIndex={-1}
                onSubmit={onSubmit}
                noValidate={As === 'form' ? true : undefined}
            >
                {(title || Icon) && (
                    <div className="modal-header">
                        {Icon && <span className={`modal-header-icon tone-${tone}`}><Icon size={20} aria-hidden="true" /></span>}
                        <div className="modal-heading">
                            {title && <h2 className="modal-title" id={titleId}>{title}</h2>}
                            {description && <p className="modal-description">{description}</p>}
                        </div>
                        {onClose && (
                            <button type="button" className="icon-btn icon-btn-sm modal-close" onClick={onClose} aria-label="Close" disabled={!dismissible}>
                                <X size={18} />
                            </button>
                        )}
                    </div>
                )}
                <div className="modal-body">{children}</div>
                {footer && <div className="modal-footer">{footer}</div>}
            </As>
        </div>,
        document.body,
    );
};

export default Modal;
