import React, { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, CheckCircle2, ClipboardCheck, ImageOff, Maximize2, RefreshCw, XCircle } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { Select, TextInput, Textarea } from './ui/Field';
import { Alert, EmptyState, Skeleton } from './ui/Feedback';
import { notifySuccess } from '../utils/notify';
import { formatDateTime, formatMoney, timeAgo } from '../utils/format';
import { getPaymentSubmissions, approvePaymentSubmission, rejectPaymentSubmission } from '../api/billingApi';
import { getBankAccounts } from '../api/adminApi';
import { viewAuthenticatedFile, useAuthenticatedMedia } from '../api/fileUtils';

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
    const screenshotUrl = selectedSub?.hasScreenshot ? `/payments/submissions/${selectedSub.id}/screenshot` : null;
    const screenshot = useAuthenticatedMedia(screenshotUrl);
    const [error, setError] = useState('');

    const loadData = useCallback(async () => {
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
            setError('We could not load the submissions. Try refreshing.');
        } finally {
            setIsLoading(false);
        }
    }, [selectedBankId]);

    useEffect(() => {
        if (isOpen) {
            loadData();
        }
    }, [isOpen, loadData]);

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
            setError('Choose the account the money was received in.');
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
            notifySuccess(`Approved ${formatMoney(selectedSub.amount, 2)} from ${selectedSub.roomNumber}`);
            setSelectedSub(null);
            await loadData();
            if (onActionSuccess) onActionSuccess();
        } catch (err) {
            setError(err?.response?.data?.message || 'We could not approve this payment. Please try again.');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleReject = async () => {
        if (!selectedSub) return;
        if (!rejectionReason.trim()) {
            setError('Tell the resident why, so they can fix it and resubmit.');
            return;
        }

        setIsProcessing(true);
        setError('');
        try {
            await rejectPaymentSubmission(selectedSub.id, {
                reason: rejectionReason.trim()
            });
            notifySuccess(`Submission from ${selectedSub.roomNumber} rejected — the resident has been told why`);
            setSelectedSub(null);
            setIsRejecting(false);
            await loadData();
            if (onActionSuccess) onActionSuccess();
        } catch (err) {
            setError(err?.response?.data?.message || 'We could not reject this submission. Please try again.');
        } finally {
            setIsProcessing(false);
        }
    };

    const pendingList = submissions.filter(s => s.status === 'PENDING');
    const modeLabel = (m) => (m || '').replace(/_MANUAL$/, '').replace(/_/g, ' ').toLowerCase().replace(/^upi/, 'UPI');

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            size="xl"
            icon={ClipboardCheck}
            title="Review payments"
            description="Check each resident’s proof of payment, then approve it into a society account or send it back."
            dismissible={!isProcessing}
            className="approvals-modal"
        >
            <div className={`split ${selectedSub ? 'has-selection' : ''}`}>
                <div className="split-list">
                    <div className="row row-between split-list-head">
                        <span className="fw-600">Waiting <span className="tab-count">{pendingList.length}</span></span>
                        <Button variant="ghost" size="sm" iconOnly icon={RefreshCw} aria-label="Refresh" onClick={loadData} />
                    </div>
                    {isLoading ? (
                        <div className="stack stack-sm">{[0, 1, 2].map((i) => <Skeleton key={i} height={68} style={{ borderRadius: 10 }} />)}</div>
                    ) : pendingList.length === 0 ? (
                        <EmptyState compact icon={CheckCircle2} title="All caught up">No payments are waiting for review.</EmptyState>
                    ) : (
                        <ul className="pick-list">
                            {pendingList.map((sub) => (
                                <li key={sub.id}>
                                    <button type="button" className={`pick ${selectedSub?.id === sub.id ? 'is-active' : ''}`} onClick={() => handleSelectSubmission(sub)}>
                                        <div className="row row-between">
                                            <span className="fw-600">{sub.roomNumber}</span>
                                            <span className="fw-600 num">{formatMoney(sub.amount, 2)}</span>
                                        </div>
                                        <div className="text-sm text-muted truncate">{sub.submittedByUserName}</div>
                                        <div className="row row-between text-xs text-subtle">
                                            <span>{modeLabel(sub.paymentMode)}{sub.advanceMonthsCount > 0 ? ` · +${sub.advanceMonthsCount} mo advance` : ''}</span>
                                            <span>{timeAgo(sub.createdAt)}</span>
                                        </div>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                <div className="split-detail">
                    {selectedSub ? (
                        <div className="stack">
                            <button type="button" className="auth-back split-back" onClick={() => setSelectedSub(null)}><ArrowLeft size={16} /> All submissions</button>
                            {error && <Alert tone="error">{error}</Alert>}
                            <div>
                                <div className="eyebrow">Submission #{selectedSub.id}</div>
                                <div className="detail-amount num">{formatMoney(selectedSub.amount, 2)}</div>
                                <div className="text-sm text-muted">From {selectedSub.submittedByUserName} · {selectedSub.roomNumber} · {formatDateTime(selectedSub.createdAt)}</div>
                            </div>
                            <dl className="kv summary-box">
                                <dt>Paid by</dt><dd>{modeLabel(selectedSub.paymentMode)}</dd>
                                <dt>Reference / UTR</dt><dd><code>{selectedSub.referenceNumber || '—'}</code></dd>
                                {selectedSub.advanceMonthsCount > 0 && <><dt>Advance</dt><dd>{selectedSub.advanceMonthsCount} months</dd></>}
                            </dl>
                            {selectedSub.residentDescription && (
                                <blockquote className="quote">“{selectedSub.residentDescription}”</blockquote>
                            )}
                            <div className="field">
                                <span className="field-label">Proof of payment</span>
                                <div className="proof">
                                    {screenshot.src ? (
                                        <button type="button" className="proof-btn" onClick={() => viewAuthenticatedFile(screenshotUrl, 'payment_proof')} aria-label="Open the screenshot full size">
                                            <img src={screenshot.src} alt="Payment proof" />
                                            <span className="proof-zoom"><Maximize2 size={14} /> Full size</span>
                                        </button>
                                    ) : screenshot.loading ? <Skeleton height={160} /> : (
                                        <span className="proof-empty"><ImageOff size={18} /> No screenshot attached</span>
                                    )}
                                </div>
                            </div>

                            {!isRejecting ? (
                                <div className="decision">
                                    <div className="form-grid">
                                        <Select label="Credit to account" value={selectedBankId} onChange={(e) => setSelectedBankId(e.target.value)}>
                                            {bankAccounts.map((acc) => (
                                                <option key={acc.id} value={acc.id}>{acc.accountName} · {formatMoney(acc.currentBalance)}</option>
                                            ))}
                                        </Select>
                                        <TextInput label="Receipt number" optional placeholder="Auto-generated" value={receiptNumber} onChange={(e) => setReceiptNumber(e.target.value)} />
                                    </div>
                                    <div className="row row-end">
                                        <Button variant="danger-ghost" icon={XCircle} onClick={() => setIsRejecting(true)} disabled={isProcessing}>Reject</Button>
                                        <Button variant="success" icon={CheckCircle2} onClick={handleApprove} isLoading={isProcessing}>Approve payment</Button>
                                    </div>
                                </div>
                            ) : (
                                <div className="decision tone-danger">
                                    <Textarea label="Why are you rejecting it?" rows={3} autoFocus value={rejectionReason} onChange={(e) => setRejectionReason(e.target.value)}
                                        placeholder="e.g. The UTR does not match our bank statement, or the screenshot is unreadable." hint="The resident sees this message." />
                                    <div className="row row-end">
                                        <Button variant="ghost" onClick={() => setIsRejecting(false)} disabled={isProcessing}>Back</Button>
                                        <Button variant="danger" onClick={handleReject} isLoading={isProcessing}>Reject submission</Button>
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="split-placeholder">
                            {error ? <Alert tone="error">{error}</Alert> : (
                                <EmptyState icon={ClipboardCheck} title="Pick a submission">Choose one from the list to see the proof and approve it.</EmptyState>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </Modal>
    );
};

export default PendingPaymentApprovalsModal;
