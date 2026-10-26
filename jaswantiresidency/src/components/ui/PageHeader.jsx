import React from 'react';

/** Page title block. `actions` (or the older `actionButton`) render on the right. */
const PageHeader = ({ title, description, eyebrow, breadcrumbs, actions, actionButton }) => (
    <header className="page-header">
        <div className="grow">
            {eyebrow && <div className="eyebrow mb-2">{eyebrow}</div>}
            {breadcrumbs && (
                <div className="breadcrumbs">
                    {breadcrumbs.map((bc, idx) => (
                        <React.Fragment key={idx}>
                            <span>{bc}</span>
                            {idx < breadcrumbs.length - 1 && <span aria-hidden="true">/</span>}
                        </React.Fragment>
                    ))}
                </div>
            )}
            <h1>{title}</h1>
            {description && <p className="page-description">{description}</p>}
        </div>
        {(actions || actionButton) && <div className="header-actions">{actions || actionButton}</div>}
    </header>
);

export default PageHeader;
