import React from 'react';
import './ui.css';

const Card = ({ children, className = '', title, ...props }) => {
    return (
        <div className={`custom-card ${className}`} {...props}>
            {title && <div className="card-header"><h3 className="card-title">{title}</h3></div>}
            <div className="card-body">
                {children}
            </div>
        </div>
    );
};

export default Card;
