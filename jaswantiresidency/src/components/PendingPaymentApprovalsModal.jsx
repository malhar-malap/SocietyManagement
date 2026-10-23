import React, { useState, useEffect } from 'react';
import Button from './ui/Button';
import StatusChip from './ui/StatusChip';
import { CheckCircle2, XCircle, Image as ImageIcon, AlertCircle, RefreshCw } from 'lucide-react';
import { getPaymentSubmissions, approvePaymentSubmission, rejectPaymentSubmission } from '../api/billingApi';
import { getBankAccounts } from '../api/adminApi';
import { viewAuthenticatedFile, getAuthenticatedMediaUrl } from '../api/fileUtils';
import './ui/shared.css';

const PendingPaymentApprovalsModal = ({ isOpen, onClose, onActionSuccess }) => {
    const [submissions, setSubmissions] = useState([]);
    const [bankAccounts, setBankAccounts] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedSub, setSelectedSub] = useState(null);
    const [selectedBankId, setSelectedBankId] = useState('');
    const [receiptNumber, setReceiptNumber] = useState('');
    const [approvalNotes, setApprovalNotes] = useState('');
    const [rejectionReason, setRejectionReason] = useState('');
    const [isRejecting, setIsRejecting] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState('');
    const [viewingScreenshotId, setViewingScreenshotId] = useState(null);

    const loadData = async () => {
        setIsLoading(true);
        setError('');
        try {
            const [subs, banks] = await Promise.all([
                getPaymentSubmissions(),
                getBankAccounts()
            ]);
            setSubmissions(subs || []);
            setBankAccounts(banks || []);
            if (banks && banks.length > 0 && !selectedBankId) {
                setSelectedBankId(banks[0].id);
            }
        } catch (err) {
            console.error('Failed to load submissions or bank accounts', err);
            setError('Failed to load pending submissions.');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            loadData();
        }
    }, [isOpen]);

    const handleSelectSubmission = (sub) => {
        setSelectedSub(sub);
        setIsRejecting(false);
        setError('');
        setRejectionReason('');
        setApprovalNotes('');
        setReceiptNumber('');
    };

    const handleApprove = async () => {
        if (!selectedSub) return;
        if (!selectedBankId) {
            setError('Please select a society bank account to credit.');
            return;
        }

        setIsProcessing(true);
        setError('');
        try {
            await approvePaymentSubmission(selectedSub.id, {
                bankAccountId: selectedBankId,
                receiptNumber: receiptNumber.trim() || undefined,
                notes: approvalNotes.trim() || undefined
            });
            setSelectedSub(null);
            await loadData();
            if (onActionSuccess) onActionSuccess();
        } catch (err) {
            setError(err?.response?.data?.message || err.message || 'Approval failed.');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleReject = async () => {
        if (!selectedSub) return;
        if (!rejectionReason.trim()) {
            setError('Please provide a reason for rejecting the payment proof.');
            return;
        }

        setIsProcessing(true);
        setError('');
        try {
            await rejectPaymentSubmission(selectedSub.id, {
                reason: rejectionReason.trim()
            });
            setSelectedSub(null);
            setIsRejecting(false);
            await loadData();
            if (onActionSuccess) onActionSuccess();
        } catch (err) {
            setError(err?.response?.data?.message || err.message || 'Rejection failed.');
        } finally {
            setIsProcessing(false);
        }
    };

    if (!isOpen) return null;

    const pendingList = submissions.filter(s => s.status === 'PENDING');

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '850px', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title">Payment Submissions Approval Queue</h2>
                        <p className="premium-modal-subtitle">
                            Verify resident transaction screenshots and credit the society bank accounts.
                        </p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <span style={{ fontSize: '1.5rem', lineHeight: 1 }}>×</span>
                    </button>
                </div>

                {error && (
                    <div style={{ margin: '1rem 1.5rem 0', padding: '0.75rem', background: 'rgba(239,68,68,0.1)', color: 'var(--danger)', borderRadius: 6, display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem' }}>
                        <AlertCircle size={16} />
                        <span>{error}</span>
                    </div>
                )}

                <div style={{ display: 'flex', flex: 1, minHeight: '380px', overflow: 'hidden', padding: '1rem 1.5rem', gap: '1.5rem' }}>
                    {/* Left: Submissions list */}
                    <div style={{ flex: '1 1 50%', overflowY: 'auto', borderRight: '1px solid var(--border-light)', paddingRight: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                            <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                                Pending Approvals ({pendingList.length})
                            </span>
                            <Button variant="secondary" onClick={loadData} style={{ padding: '0.2rem 0.5rem' }}>
                                <RefreshCw size={14} />
                            </Button>
                        </div>

                        {isLoading ? (
                            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary-light)' }}>Loading...</div>
                        ) : pendingList.length === 0 ? (
                            <div style={{ padding: '3rem 1rem', textAlign: 'center', color: 'var(--text-secondary-light)' }}>
                                No pending payment submissions to review.
                            </div>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                {pendingList.map(sub => (
                                    <div
                                        key={sub.id}
                                        onClick={() => handleSelectSubmission(sub)}
                                        style={{
                                            padding: '0.75rem',
                                            borderRadius: 8,
                                            border: selectedSub?.id === sub.id ? '2px solid var(--primary)' : '1px solid var(--border-light)',
                                            background: selectedSub?.id === sub.id ? '#f0f7ff' : '#fff',
                                            cursor: 'pointer',
                                            transition: 'all 0.15s ease'
                                        }}
                                    >
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Room {sub.roomNumber} - {sub.submittedByUserName}</span>
                                            <span style={{ fontWeight: 700, color: 'var(--success)', fontSize: '0.95rem' }}>₹{Number(sub.amount).toFixed(2)}</span>
                                        </div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)', marginTop: '0.25rem' }}>
                                            Mode: {sub.paymentMode} | Ref: {sub.referenceNumber || 'N/A'}
                                        </div>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.4rem', fontSize: '0.75rem' }}>
                                            <span style={{ color: 'var(--text-secondary-light)' }}>
                                                {new Date(sub.createdAt).toLocaleDateString()} {new Date(sub.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                            {sub.advanceMonthsCount > 0 && (
                                                <span style={{ background: '#e0e7ff', color: '#3730a3', padding: '0.1rem 0.4rem', borderRadius: 4, fontWeight: 600 }}>
                                                    +{sub.advanceMonthsCount} mo adv
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Right: Selected submission details and action */}
                    <div style={{ flex: '1 1 50%', overflowY: 'auto' }}>
                        {selectedSub ? (
                            <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                                <div style={{ borderBottom: '1px solid var(--border-light)', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
                                    <h4 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600 }}>
                                        Submission #{selectedSub.id} - Room {selectedSub.roomNumber}
                                    </h4>
                                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary-light)', marginTop: '0.2rem' }}>
                                        Submitted by {selectedSub.submittedByUserName}
                                    </div>
                                </div>

                                <div style={{ background: '#f8fafc', padding: '0.75rem', borderRadius: 6, fontSize: '0.85rem', marginBottom: '1rem' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                                        <span>Amount:</span>
                                        <strong style={{ fontSize: '1rem', color: 'var(--success)' }}>₹{Number(selectedSub.amount).toFixed(2)}</strong>
                                    </div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                                        <span>Mode:</span>
                                        <strong>{selectedSub.paymentMode}</strong>
                                    </div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                                        <span>Reference / UTR:</span>
                                        <strong style={{ fontFamily: 'monospace' }}>{selectedSub.referenceNumber || 'N/A'}</strong>
                                    </div>
                                    {selectedSub.advanceMonthsCount > 0 && (
                                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                                            <span>Advance Period:</span>
                                            <strong style={{ color: 'var(--primary)' }}>+{selectedSub.advanceMonthsCount} months</strong>
                                        </div>
                                    )}
                                    {selectedSub.residentDescription && (
                                        <div style={{ marginTop: '0.5rem', borderTop: '1px solid #e2e8f0', paddingTop: '0.5rem' }}>
                                            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)', display: 'block' }}>Resident Note:</span>
                                            <em>"{selectedSub.residentDescription}"</em>
                                        </div>
                                    )}
                                </div>

                                {/* Screenshot Viewer */}
                                <div style={{ marginBottom: '1rem' }}>
                                    <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.4rem' }}>
                                        Attached Screenshot / Proof:
                                    </label>
                                    <div style={{ border: '1px solid var(--border-light)', borderRadius: 6, overflow: 'hidden', textAlign: 'center', background: '#f1f5f9', padding: '0.5rem' }}>
                                        <img
                                            src={getAuthenticatedMediaUrl(`/payments/submissions/${selectedSub.id}/screenshot`)}
                                            alt="Payment Proof Screenshot"
                                            style={{ maxWidth: '100%', maxHeight: '180px', objectFit: 'contain', cursor: 'pointer' }}
                                            onClick={() => viewAuthenticatedFile(`/payments/submissions/${selectedSub.id}/screenshot`)}
                                            onError={(e) => {
                                                e.target.onerror = null;
                                                e.target.style.display = 'none';
                                                e.target.parentNode.innerHTML = '<span style="font-size:0.8rem;color:#64748b;padding:1rem;display:block;">No screenshot available or unable to load</span>';
                                            }}
                                        />
                                        <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary-light)', marginTop: '0.2rem' }}>
                                            (Click to open full resolution in new tab)
                                        </div>
                                    </div>
                                </div>

                                {/* Action Area */}
                                {!isRejecting ? (
                                    <div style={{ marginTop: 'auto', borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem' }}>
                                        <div style={{ marginBottom: '0.75rem' }}>
                                            <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.25rem' }}>
                                                Select Destination Bank Account <span style={{ color: 'red' }}>*</span>
                                            </label>
                                            <select
                                                value={selectedBankId}
                                                onChange={(e) => setSelectedBankId(e.target.value)}
                                                style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                                            >
                                                {bankAccounts.map(acc => (
                                                    <option key={acc.id} value={acc.id}>
                                                        {acc.accountName} (Balance: ₹{Number(acc.currentBalance).toFixed(2)})
                                                    </option>
                                                ))}
                                            </select>
                                        </div>

                                        <div style={{ marginBottom: '0.75rem' }}>
                                            <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.25rem' }}>
                                                Receipt Number (Optional, auto-generated if blank)
                                            </label>
                                            <input
                                                type="text"
                                                placeholder="e.g. REC-2026-0042"
                                                value={receiptNumber}
                                                onChange={(e) => setReceiptNumber(e.target.value)}
                                                style={{ width: '100%', padding: '0.45rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                                            />
                                        </div>

                                        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                                            <Button
                                                onClick={handleApprove}
                                                disabled={isProcessing}
                                                style={{ flex: 1, background: 'var(--success)' }}
                                            >
                                                <CheckCircle2 size={16} /> {isProcessing ? 'Processing...' : 'Approve & Credit'}
                                            </Button>
                                            <Button
                                                variant="secondary"
                                                onClick={() => setIsRejecting(true)}
                                                disabled={isProcessing}
                                                style={{ color: 'var(--danger)', borderColor: 'var(--danger)' }}
                                            >
                                                <XCircle size={16} /> Reject
                                            </Button>
                                        </div>
                                    </div>
                                ) : (
                                    <div style={{ marginTop: 'auto', borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem' }}>
                                        <div style={{ marginBottom: '0.75rem' }}>
                                            <label style={{ fontSize: '0.8rem', fontWeight: 600, display: 'block', marginBottom: '0.25rem', color: 'var(--danger)' }}>
                                                Rejection Reason <span style={{ color: 'red' }}>*</span>
                                            </label>
                                            <textarea
                                                rows={2}
                                                placeholder="Explain why this proof is rejected (e.g. invalid UTR, blurred screenshot, mismatching amount)..."
                                                value={rejectionReason}
                                                onChange={(e) => setRejectionReason(e.target.value)}
                                                style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--danger)', fontSize: '0.85rem' }}
                                            />
                                        </div>

                                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                                            <Button
                                                onClick={handleReject}
                                                disabled={isProcessing}
                                                style={{ flex: 1, background: 'var(--danger)' }}
                                            >
                                                <XCircle size={16} /> {isProcessing ? 'Rejecting...' : 'Confirm Rejection'}
                                            </Button>
                                            <Button
                                                variant="secondary"
                                                onClick={() => setIsRejecting(false)}
                                                disabled={isProcessing}
                                            >
                                                Cancel
                                            </Button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div style={{ padding: '4rem 1rem', textAlign: 'center', color: 'var(--text-secondary-light)' }}>
                                Select a pending submission from the list on the left to review details and approve.
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PendingPaymentApprovalsModal;
