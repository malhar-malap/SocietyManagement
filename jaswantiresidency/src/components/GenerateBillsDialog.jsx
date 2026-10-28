import React, { useEffect, useState } from 'react';
import { Receipt } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput } from './ui/Field';
import { Alert } from './ui/Feedback';
import { notifySuccess, errorMessage } from '../utils/notify';
import { formatMoney, monthLabel } from '../utils/format';

const pad = (n) => String(n).padStart(2, '0');
const defaults = () => {
    const d = new Date();
    return { billingMonth: `${d.getFullYear()}-${pad(d.getMonth() + 1)}`, amount: '2000', dueDate: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-10` };
};

const GenerateBillsDialog = ({ isOpen, onClose, onConfirm }) => {
    const [formData, setFormData] = useState(defaults);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => { if (isOpen) { setFormData(defaults()); setError(''); } }, [isOpen]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.billingMonth || !formData.dueDate) { setError('Choose the billing month and a due date.'); return; }
        if (!(Number(formData.amount) > 0)) { setError('Enter the maintenance amount per home.'); return; }
        setIsLoading(true);
        try {
            await onConfirm(formData);
            notifySuccess(`Bills generated for ${monthLabel(formData.billingMonth)}`);
            onClose();
        } catch (err) {
            setError(errorMessage(err, 'We could not generate the bills. Please try again.'));
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            as="form"
            onSubmit={handleSubmit}
            icon={Receipt}
            title="Generate monthly bills"
            description="Creates a maintenance bill for every occupied home. Units with an NOC charge also get their NOC bill."
            dismissible={!isLoading}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isLoading}>Cancel</Button>
                    <Button type="submit" isLoading={isLoading}>Generate bills</Button>
                </>
            )}
        >
            <div className="stack">
                {error && <Alert tone="error">{error}</Alert>}
                <div className="form-grid">
                    <TextInput label="Billing month" type="month" name="billingMonth" value={formData.billingMonth} onChange={handleChange} />
                    <TextInput label="Due date" type="date" name="dueDate" value={formData.dueDate} onChange={handleChange} />
                </div>
                <TextInput label="Maintenance per home" prefix="₹" type="number" name="amount" min="1" placeholder="2000" value={formData.amount} onChange={handleChange} />
                {Number(formData.amount) > 0 && formData.billingMonth && (
                    <p className="text-sm text-muted">Each home will be billed <strong className="num" style={{ color: 'var(--text)' }}>{formatMoney(formData.amount)}</strong> for {monthLabel(formData.billingMonth)}. Homes already billed for this month are skipped.</p>
                )}
            </div>
        </Modal>
    );
};

export default GenerateBillsDialog;
