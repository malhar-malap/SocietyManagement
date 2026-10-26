import React from 'react';

/** Surface with an optional header (title, subtitle, action) and footer. */
const Card = ({ children, className = '', title, subtitle, action, footer, flush = false, divider = false, as: As = 'section', ...props }) => (
    <As className={`card ${className}`} {...props}>
        {(title || action) && (
            <div className={`card-header ${divider ? 'with-divider' : ''}`}>
                <div className="grow">
                    {title && <h3 className="card-title">{title}</h3>}
                    {subtitle && <p className="card-subtitle">{subtitle}</p>}
                </div>
                {action}
            </div>
        )}
        <div className={`card-body ${flush ? 'flush' : ''}`}>{children}</div>
        {footer && <div className="card-footer">{footer}</div>}
    </As>
);

export default Card;
