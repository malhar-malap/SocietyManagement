import React, { useState, useEffect } from 'react';
import { MessageSquareReply } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { Textarea } from './ui/Field';
import { Alert } from './ui/Feedback';
import TicketCard, { TYPE_LABELS } from './helpdesk/TicketCard';

const STATUS_OPTIONS = [
    { value: 'OPEN', label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In progress' },
    { value: 'RESOLVED', label: 'Resolved' },
];
const PERMISSION_OPTIONS = [
    { value: 'OPEN', label: 'Under review' },
    { value: 'APPROVED', label: 'Approve' },
    { value: 'REJECTED', label: 'Decline' },
    { value: 'RESOLVED', label: 'Completed' },
];

const UpdateComplaintModal = ({ isOpen, onClose, onConfirm, complaintDetails }) => {
    const [status, setStatus] = useState('OPEN');
    const [adminResponse, setAdminResponse] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (complaintDetails) {
            setStatus(complaintDetails.status || 'OPEN');
            setAdminResponse(complaintDetails.adminResponse || '');
        }
        setError('');
    }, [complaintDetails, isOpen]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');
        try {
            await onConfirm(complaintDetails.id, status, adminResponse.trim());
            onClose();
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    const isPermission = complaintDetails?.submissionType === 'PERMISSION';
    const options = isPermission ? PERMISSION_OPTIONS : STATUS_OPTIONS;

    return (
        <Modal
            isOpen={isOpen && Boolean(complaintDetails)}
            onClose={onClose}
            as="form"
            onSubmit={handleSubmit}
            size="lg"
            icon={MessageSquareReply}
            title={`Reply to ${(TYPE_LABELS[complaintDetails?.submissionType] || 'request').toLowerCase()} #${complaintDetails?.id || ''}`}
            description={complaintDetails ? `${complaintDetails.roomNumber || ''} · ${complaintDetails.residentName || 'Resident'}` : undefined}
            dismissible={!isLoading}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isLoading}>Cancel</Button>
                    <Button type="submit" isLoading={isLoading}>Send reply</Button>
                </>
            )}
        >
            {complaintDetails && (
                <div className="stack">
                    {error && <Alert tone="error">{error}</Alert>}
                    <TicketCard ticket={{ ...complaintDetails, adminResponse: null }} />
                    <div className="field">
                        <span className="field-label">{isPermission ? 'Decision' : 'Status'}</span>
                        <div className="choice-grid" role="radiogroup" aria-label={isPermission ? 'Decision' : 'Status'}>
                            {options.map((o) => (
                                <button key={o.value} type="button" role="radio" aria-checked={status === o.value} className="choice" style={{ justifyContent: 'center' }} onClick={() => setStatus(o.value)}>
                                    {o.label}
                                </button>
                            ))}
                        </div>
                    </div>
                    <Textarea label="Message to the resident" optional rows={4} value={adminResponse} onChange={(e) => setAdminResponse(e.target.value)}
                        placeholder={isPermission ? 'Any conditions, e.g. work allowed 10 am – 6 pm only.' : 'What has been done, or what happens next.'} />
                </div>
            )}
        </Modal>
    );
};

export default UpdateComplaintModal;
