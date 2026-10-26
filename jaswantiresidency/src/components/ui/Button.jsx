import React from 'react';

/**
 * variant: primary | secondary | ghost | danger | danger-ghost | success | soft | link
 * size: sm | md | lg. `icon` renders before the label; `iconOnly` makes a square button (pass aria-label).
 */
const Button = React.forwardRef(({
    children, variant = 'primary', size = 'md', block = false, icon: Icon, iconOnly = false,
    className = '', isLoading = false, type = 'button', disabled, ...props
}, ref) => {
    const classes = [
        'btn', `btn-${variant}`, size !== 'md' && `btn-${size}`, block && 'btn-block',
        iconOnly && 'btn-icon', isLoading && 'is-loading', className,
    ].filter(Boolean).join(' ');
    const iconSize = size === 'sm' ? 15 : 17;
    return (
        <button ref={ref} type={type} className={classes} disabled={isLoading || disabled} aria-busy={isLoading || undefined} {...props}>
            <span className="btn-label">
                {Icon && <Icon size={iconSize} aria-hidden="true" />}
                {!iconOnly && children}
            </span>
            {isLoading && <span className="btn-spinner" aria-hidden="true" />}
        </button>
    );
});

Button.displayName = 'Button';
export default Button;
