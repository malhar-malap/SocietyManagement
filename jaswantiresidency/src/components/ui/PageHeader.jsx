import React from 'react';
import './ui.css';

const PageHeader = ({ title, breadcrumbs, actionButton }) => {
    return (
        <div className="page-header">
            <div className="header-content">
                {breadcrumbs && (
                    <div className="breadcrumbs">
                        {breadcrumbs.map((bc, idx) => (
                            <span key={idx}>
                                {bc}
                                {idx < breadcrumbs.length - 1 && <span className="separator">/</span>}
                            </span>
                        ))}
                    </div>
                )}
                <h1 className="page-title">{title}</h1>
            </div>
            {actionButton && (
                <div className="header-actions">
                    {actionButton}
                </div>
            )}
        </div>
    );
};

export default PageHeader;
