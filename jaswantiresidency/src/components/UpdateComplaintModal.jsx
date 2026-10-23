import React, { useState, useEffect } from 'react';
import { X, CheckCircle } from 'lucide-react';
import './ui/shared.css';
import { viewAuthenticatedFile } from '../api/fileUtils';

const UpdateComplaintModal = ({ isOpen, onClose, onConfirm, complaintDetails }) => {
    const [status, setStatus] = useState('OPEN');
    const [adminResponse, setAdminResponse] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (complaintDetails) {
            setStatus(complaintDetails.status || 'OPEN');
            setAdminResponse(complaintDetails.adminResponse || '');
        }
    }, [complaintDetails, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await onConfirm(complaintDetails.id, status, adminResponse);
            onClose();
        } catch (error) {
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    const isPermission = complaintDetails?.submissionType === 'PERMISSION';

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '480px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.25rem' }}>
                            Respond to {complaintDetails?.submissionType || 'Complaint'} #{complaintDetails?.id}
                        </h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} style={{ padding: '1rem 1.5rem' }}>
                    <div style={{ background: '#f8fafc', padding: '0.85rem', borderRadius: 8, fontSize: '0.85rem', marginBottom: '1rem' }}>
                        <div><strong>Resident:</strong> {complaintDetails?.residentName || 'N/A'} (Room {complaintDetails?.roomNumber || 'N/A'})</div>
                        <div style={{ marginTop: '0.25rem' }}><strong>Description:</strong> {complaintDetails?.description}</div>
                        {isPermission && (
                            <div style={{ marginTop: '0.25rem', color: 'var(--primary)' }}>
                                <strong>Dates:</strong> {complaintDetails?.startDate || 'N/A'} to {complaintDetails?.endDate || 'N/A'}
                            </div>
                        )}
                        {complaintDetails?.hasAttachment && (
                            <div style={{ marginTop: '0.4rem' }}>
                                <button
                                    type="button"
                                    onClick={() => viewAuthenticatedFile(`/complaints/${complaintDetails?.id}/attachment`)}
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, color: 'var(--primary)', fontWeight: 600, fontSize: '0.8rem' }}
                                >
                                    View Attached File / Proof
                                </button>
                            </div>
                        )}
                    </div>

                    <div style={{ marginBottom: '1rem' }}>
                        <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                            Decision / Status <span style={{ color: 'red' }}>*</span>
                        </label>
                        <select
                            className="custom-input"
                            style={{ width: '100%', padding: '0.5rem' }}
                            value={status}
                            onChange={(e) => setStatus(e.target.value)}
                            required
                        >
                            {isPermission ? (
                                <>
                                    <option value="OPEN">Under Review (OPEN)</option>
                                    <option value="IN_PROGRESS">In Progress</option>
                                    <option value="APPROVED">Approved</option>
                                    <option value="REJECTED">Rejected</option>
                                    <option value="RESOLVED">Resolved / Completed</option>
                                </>
                            ) : (
                                <>
                                    <option value="OPEN">Open</option>
                                    <option value="IN_PROGRESS">In Progress</option>
                                    <option value="RESOLVED">Resolved</option>
                                </>
                            )}
                        </select>
                    </div>

                    <div style={{ marginBottom: '1.25rem' }}>
                        <label className="input-label" style={{ display: 'block', marginBottom: '0.3rem' }}>
                            Official Admin Response / Notes
                        </label>
                        <textarea
                            className="custom-input"
                            style={{ width: '100%', padding: '0.5rem' }}
                            rows="3"
                            placeholder="Enter message for resident regarding approval conditions or resolution..."
                            value={adminResponse}
                            onChange={(e) => setAdminResponse(e.target.value)}
                        />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>
                            Cancel
                        </button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading ? 'Saving...' : <><CheckCircle size={15} /> Save Response</>}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UpdateComplaintModal;
