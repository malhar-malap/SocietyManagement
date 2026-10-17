import React from 'react';
import './ui.css';

const Button = ({ children, variant = 'primary', className = '', isLoading, ...props }) => {
    return (
        <button
            className={`custom-btn btn-${variant} ${isLoading ? 'btn-loading' : ''} ${className}`}
            disabled={isLoading || props.disabled}
            {...props}
        >
            {isLoading ? <span className="spinner"></span> : children}
        </button>
    );
};

export default Button;
