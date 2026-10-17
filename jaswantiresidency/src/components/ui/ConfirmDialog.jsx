import React from 'react';
import Button from './Button';
import './ui.css';

const ConfirmDialog = ({ isOpen, title, message, onConfirm, onCancel, confirmText = 'Confirm', confirmVariant = 'danger', isLoading = false }) => {
    if (!isOpen) return null;

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '400px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.25rem' }}>{title}</h2>
                    </div>
                </div>
                <p className="premium-modal-subtitle" style={{ marginBottom: '2rem', marginTop: 0 }}>{message}</p>
                <div className="premium-actions">
                    <Button variant="secondary" onClick={onCancel} disabled={isLoading}>Cancel</Button>
                    <Button variant={confirmVariant} onClick={onConfirm} isLoading={isLoading}>{confirmText}</Button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmDialog;
