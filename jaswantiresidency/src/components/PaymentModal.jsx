import React, { useState } from 'react';
import { ArrowLeft, Banknote, Building2, Check, CheckCircle2, Copy, Smartphone, Wallet } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { FileInput, TextInput, Textarea } from './ui/Field';
import { Alert } from './ui/Feedback';
import { useAuth } from '../context/AuthContext';
import { formatMoney, monthLabel } from '../utils/format';

const SOCIETY_ACCOUNT = [
    { label: 'UPI ID', value: 'jaswanti.residency@hdfcbank' },
    { label: 'Account name', value: 'Jaswanti Residency CHS Maintenance' },
    { label: 'Account number', value: '50100456789123' },
    { label: 'IFSC', value: 'HDFC0001234' },
];

const MODES = [
    { value: 'UPI_MANUAL', label: 'UPI', hint: 'GPay, PhonePe, Paytm', icon: Smartphone },
    { value: 'BANK_TRANSFER', label: 'Bank transfer', hint: 'NEFT or IMPS', icon: Building2 },
    { value: 'CASH', label: 'Cash', hint: 'At the society office', icon: Banknote },
];

const ADVANCE = [
    { value: 0, label: 'Dues only' },
    { value: 1, label: '+1 month' },
    { value: 3, label: '+3 months' },
    { value: 6, label: '+6 months' },
    { value: 11, label: 'Full year' },
];

const CopyRow = ({ label, value }) => {
    const [copied, setCopied] = useState(false);
    return (
        <div className="copy-row">
            <span className="text-subtle text-xs">{label}</span>
            <span className="copy-value">{value}</span>
            <button type="button" className="icon-btn icon-btn-sm" aria-label={`Copy ${label}`}
                onClick={async () => { try { await navigator.clipboard.writeText(value); setCopied(true); setTimeout(() => setCopied(false), 1500); } catch { /* ignore */ } }}>
                {copied ? <Check size={15} className="text-success" /> : <Copy size={15} />}
            </button>
        </div>
    );
};

/**
 * Residents pay the society directly (UPI / bank / cash) and then tell us about it here; an admin checks the
 * proof and the bill is marked paid. Step 1 shows what to pay and where, step 2 collects the proof.
 */
const PaymentModal = ({ amount, bills, onClose, onSuccess }) => {
    const { user } = useAuth();
    const [step, setStep] = useState('pay');
    const [paymentMode, setPaymentMode] = useState('UPI_MANUAL');
    const [referenceNumber, setReferenceNumber] = useState('');
    const [advanceMonths, setAdvanceMonths] = useState(0);
    const [screenshotFile, setScreenshotFile] = useState(null);
    const [description, setDescription] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [submittedData, setSubmittedData] = useState(null);

    const baseAmount = parseFloat(amount) || (bills && bills.length > 0 ? parseFloat(bills[0].amount || 0) : 3500);
    const totalAmount = (baseAmount * (1 + parseInt(advanceMonths || 0, 10))).toFixed(2);
    const roomId = bills && bills.length > 0 ? bills[0].roomId : (user?.roomId || 1);
    const months = (bills || []).map((b) => monthLabel(b.billingMonth, 'short')).filter(Boolean);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (paymentMode !== 'CASH' && !referenceNumber.trim()) {
            setError(paymentMode === 'UPI_MANUAL' ? 'Enter the UPI reference number from your payment app.' : 'Enter the UTR / reference number of your transfer.');
            return;
        }

        setIsSubmitting(true);
        setError('');
        try {
            const { submitResidentPayment } = await import('../api/billingApi');
            const formData = new FormData();
            formData.append('roomId', roomId);
            formData.append('amount', totalAmount);
            formData.append('paymentMode', paymentMode);
            if (referenceNumber.trim()) formData.append('referenceNumber', referenceNumber.trim());
            if (description.trim()) formData.append('residentDescription', description.trim());
            formData.append('advanceMonthsCount', advanceMonths);

            if (bills && bills.length > 0) {
                formData.append('selectedBillIds', bills.map((b) => b.id));
            }
            if (screenshotFile) {
                formData.append('screenshot', screenshotFile);
            }

            const res = await submitResidentPayment(formData);
            setSubmittedData(res);
            if (onSuccess) onSuccess(res);
        } catch (err) {
            setError(err?.response?.data?.message || 'We could not send your payment details. Please try again.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (submittedData) {
        return (
            <Modal onClose={onClose} size="sm" footer={<Button block onClick={onClose}>Done</Button>}>
                <div className="receipt">
                    <span className="receipt-icon"><CheckCircle2 size={28} /></span>
                    <h2 className="modal-title">Thanks — we’ve got it</h2>
                    <p className="text-sm text-muted">The society office will check your payment, usually within a day. Your bill updates and you get a receipt once it is approved.</p>
                    <div className="receipt-amount num">{formatMoney(submittedData.amount, 2)}</div>
                    <dl className="kv summary-box" style={{ textAlign: 'left' }}>
                        <dt>Reference</dt><dd>{submittedData.referenceNumber || '—'}</dd>
                        <dt>Paid by</dt><dd>{MODES.find((m) => m.value === submittedData.paymentMode)?.label || submittedData.paymentMode}</dd>
                        {submittedData.advanceMonthsCount > 0 && <><dt>Advance</dt><dd>{submittedData.advanceMonthsCount} months</dd></>}
                        <dt>Status</dt><dd>Waiting for approval</dd>
                    </dl>
                </div>
            </Modal>
        );
    }

    return (
        <Modal
            onClose={onClose}
            as="form"
            onSubmit={step === 'pay' ? (e) => { e.preventDefault(); setStep('proof'); } : handleSubmit}
            icon={Wallet}
            title={step === 'pay' ? 'Pay maintenance' : 'Tell us about your payment'}
            description={step === 'pay' ? 'Pay the society directly, then confirm it here.' : 'The office uses this to match your payment.'}
            dismissible={!isSubmitting}
            footer={step === 'pay' ? (
                <>
                    <span className="footer-start">Step 1 of 2</span>
                    <Button variant="secondary" onClick={onClose}>Cancel</Button>
                    <Button type="submit">I’ve paid {formatMoney(totalAmount)}</Button>
                </>
            ) : (
                <>
                    <Button variant="ghost" icon={ArrowLeft} onClick={() => { setStep('pay'); setError(''); }} disabled={isSubmitting} className="footer-back">Back</Button>
                    <Button type="submit" isLoading={isSubmitting}>Send for approval</Button>
                </>
            )}
        >
            {step === 'pay' ? (
                <div className="stack">
                    <div className="pay-total">
                        <span className="text-sm text-muted">Amount to pay</span>
                        <strong className="num">{formatMoney(totalAmount, 2)}</strong>
                        {months.length > 0 && <span className="text-xs text-subtle">For {months.join(', ')}{advanceMonths > 0 ? ` + ${advanceMonths} month${advanceMonths > 1 ? 's' : ''} in advance` : ''}</span>}
                    </div>
                    <div className="field">
                        <span className="field-label">Pay ahead?</span>
                        <div className="segmented" role="group" aria-label="Advance months">
                            {ADVANCE.map((a) => (
                                <button key={a.value} type="button" aria-pressed={advanceMonths === a.value} onClick={() => setAdvanceMonths(a.value)}>{a.label}</button>
                            ))}
                        </div>
                    </div>
                    <div className="field">
                        <span className="field-label">Society account</span>
                        <div className="copy-list">
                            {SOCIETY_ACCOUNT.map((r) => <CopyRow key={r.label} {...r} />)}
                        </div>
                        <span className="field-hint">Or pay in cash at the society office and keep the slip.</span>
                    </div>
                </div>
            ) : (
                <div className="stack">
                    {error && <Alert tone="error">{error}</Alert>}
                    <div className="field">
                        <span className="field-label">How did you pay?</span>
                        <div className="choice-grid" role="radiogroup" aria-label="Payment method">
                            {MODES.map((m) => (
                                <button key={m.value} type="button" role="radio" aria-checked={paymentMode === m.value} className="choice choice-tall"
                                    onClick={() => { setPaymentMode(m.value); setError(''); }}>
                                    <m.icon size={18} aria-hidden="true" />
                                    <span><span className="choice-label">{m.label}</span><span className="choice-hint">{m.hint}</span></span>
                                </button>
                            ))}
                        </div>
                    </div>
                    <TextInput
                        label={paymentMode === 'UPI_MANUAL' ? 'UPI reference number' : paymentMode === 'BANK_TRANSFER' ? 'UTR / reference number' : 'Receipt slip number'}
                        optional={paymentMode === 'CASH'}
                        placeholder={paymentMode === 'UPI_MANUAL' ? '12-digit number from your UPI app' : paymentMode === 'BANK_TRANSFER' ? 'From your bank’s confirmation' : ''}
                        value={referenceNumber}
                        onChange={(e) => { setReferenceNumber(e.target.value); setError(''); }}
                        autoFocus
                    />
                    <FileInput label="Screenshot or receipt" optional={paymentMode === 'CASH'} accept="image/*,.pdf" file={screenshotFile} onChange={setScreenshotFile}
                        hint="A screenshot of the payment helps the office approve it faster." />
                    <Textarea label="Note for the office" optional rows={2} placeholder="e.g. Paid from my wife’s account for Oct–Dec" value={description} onChange={(e) => setDescription(e.target.value)} />
                    <div className="summary-box row row-between"><span className="text-muted">Amount</span><strong className="num">{formatMoney(totalAmount, 2)}</strong></div>
                </div>
            )}
        </Modal>
    );
};

export default PaymentModal;
