import React, { useState } from 'react';
import { X, CheckCircle } from 'lucide-react';
import './ui/shared.css';

const UpdateComplaintModal = ({ isOpen, onClose, onConfirm, complaintDetails }) => {
    const [status, setStatus] = useState(complaintDetails?.status || 'OPEN');
    const [isLoading, setIsLoading] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await onConfirm(complaintDetails.id, status);
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
                        <h2 className="premium-modal-title" style={{ fontSize: '1.25rem' }}>Update Complaint</h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <p className="premium-modal-subtitle" style={{ marginBottom: '1.5rem', fontSize: '0.95rem' }}>
                        Updating status for complaint <strong style={{ color: 'var(--text-primary-light)' }}>{complaintDetails?.id}</strong> from room <strong style={{ color: 'var(--text-primary-light)' }}>{complaintDetails?.roomId}</strong>.
                    </p>

                    <div className="input-group-modern">
                        <label className="input-label">New Status</label>
                        <select 
                            className="modern-input"
                            value={status}
                            onChange={(e) => setStatus(e.target.value)}
                            style={{ paddingLeft: '1.2rem' }}
                            required
                        >
                            <option value="OPEN">Open</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="RESOLVED">Resolved</option>
                        </select>
                    </div>

                    <div className="premium-actions">
                        <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>Cancel</button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading ? <span className="spinner"></span> : <><CheckCircle size={16} /> Update Status</>}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UpdateComplaintModal;
