import React from 'react';
import { X, AlertTriangle } from 'lucide-react';
import './ui/shared.css';

const DeleteConfirmationDialog = ({ isOpen, onClose, onConfirm, title, itemName, isDeleting }) => {
    if (!isOpen) return null;

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '450px' }}>
                <div className="premium-modal-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--danger)' }}>
                        <AlertTriangle size={28} />
                        <h2 className="premium-modal-title" style={{ fontSize: '1.5rem', background: 'none', WebkitTextFillColor: 'var(--danger)' }}>{title}</h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>
                
                <p className="premium-modal-subtitle" style={{ marginBottom: '2.5rem', fontSize: '1rem' }}>
                    Are you sure you want to delete <strong>{itemName}</strong>? This action cannot be undone and will permanently remove the data from our servers.
                </p>

                <div className="premium-actions">
                    <button 
                        className="custom-btn secondary" 
                        onClick={onClose}
                        disabled={isDeleting}
                    >
                        Cancel
                    </button>
                    <button 
                        className="custom-btn primary" 
                        style={{ background: 'linear-gradient(135deg, #ef4444, #dc2626)', boxShadow: '0 4px 14px 0 rgba(239, 68, 68, 0.39)', borderColor: 'rgba(239, 68, 68, 0.5)' }}
                        onClick={onConfirm}
                        disabled={isDeleting}
                    >
                        {isDeleting ? <span className="spinner"></span> : 'Delete'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DeleteConfirmationDialog;
