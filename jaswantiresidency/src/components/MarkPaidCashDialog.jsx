import React, { useState } from 'react';
import { X, CheckCircle2 } from 'lucide-react';
import './ui/shared.css';

const MarkPaidCashDialog = ({ isOpen, onClose, onConfirm, billDetails }) => {
    const [isLoading, setIsLoading] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await onConfirm(billDetails.id);
            onClose();
        } catch (error) {
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '400px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.25rem' }}>Confirm Cash Payment</h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>

                <p className="premium-modal-subtitle" style={{ marginBottom: '2rem' }}>
                    Are you sure you want to mark this bill as paid by cash? 
                    <br/><br/>
                    <strong style={{ color: 'var(--text-primary-light)' }}>Room:</strong> {billDetails?.room} <br/>
                    <strong style={{ color: 'var(--text-primary-light)' }}>Amount:</strong> ₹{billDetails?.amount}
                </p>

                <div className="premium-actions">
                    <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>Cancel</button>
                    <button onClick={handleSubmit} className="custom-btn primary" style={{ backgroundColor: 'var(--success)', border: 'none' }} disabled={isLoading}>
                        {isLoading ? <span className="spinner"></span> : <><CheckCircle2 size={16} /> Mark as Paid</>}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MarkPaidCashDialog;
