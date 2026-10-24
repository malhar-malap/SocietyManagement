import React from 'react';
import './ui.css';

// `action` renders in the card header (e.g. a "View All" link); it used to fall through to the <div> as an
// invalid attribute, so the link never showed and React logged a warning
const Card = ({ children, className = '', title, action, ...props }) => {
    return (
        <div className={`custom-card ${className}`} {...props}>
            {(title || action) && (
                <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
                    {title && <h3 className="card-title">{title}</h3>}
                    {action}
                </div>
            )}
            <div className="card-body">
                {children}
            </div>
        </div>
    );
};

export default Card;
