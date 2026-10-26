import React, { useEffect, useState } from 'react';
import { HelpCircle, Lightbulb, MessageSquarePlus, ShieldCheck, Wrench } from 'lucide-react';
import Modal from '../ui/Modal';
import Button from '../ui/Button';
import { FileInput, Select, TextInput, Textarea } from '../ui/Field';
import { Alert } from '../ui/Feedback';

const TYPES = [
    { value: 'COMPLAINT', label: 'Something needs fixing', hint: 'Leaks, lights, lifts, cleaning', icon: Wrench },
    { value: 'QUERY', label: 'Ask a question', hint: 'Bills, rules, documents', icon: HelpCircle },
    { value: 'SUGGESTION', label: 'Share an idea', hint: 'Make the society better', icon: Lightbulb },
    { value: 'PERMISSION', label: 'Ask for permission', hint: 'Renovation, moving, NOC letters', icon: ShieldCheck },
];

const CATEGORIES = [
    { value: 'Maintenance', label: 'General maintenance' },
    { value: 'Plumbing', label: 'Plumbing' },
    { value: 'Electrical', label: 'Electrical' },
    { value: 'Security', label: 'Security and parking' },
    { value: 'Accounts', label: 'Accounts and billing' },
    { value: 'Other', label: 'Other' },
];

const PERMISSIONS = [
    { value: 'RENOVATION', label: 'Flat renovation or civil work' },
    { value: 'INTERIOR_WORK', label: 'Carpentry and painting' },
    { value: 'MOVE_IN_OUT', label: 'Moving in or out' },
    { value: 'ADDRESS_PROOF', label: 'Address proof or NOC letter' },
    { value: 'OTHER', label: 'Something else' },
];

const EMPTY = { category: 'Maintenance', title: '', description: '', permissionCategory: 'RENOVATION', startDate: '', endDate: '' };

/** Residents raise a request in two quick decisions: what kind, then the details. */
const NewRequestModal = ({ isOpen, onClose, onSubmit }) => {
    const [type, setType] = useState('COMPLAINT');
    const [form, setForm] = useState(EMPTY);
    const [file, setFile] = useState(null);
    const [errors, setErrors] = useState({});
    const [formError, setFormError] = useState('');
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (isOpen) { setType('COMPLAINT'); setForm(EMPTY); setFile(null); setErrors({}); setFormError(''); }
    }, [isOpen]);

    const set = (k) => (e) => { setForm((f) => ({ ...f, [k]: e.target.value })); setErrors((er) => ({ ...er, [k]: undefined })); };

    const submit = async (e) => {
        e.preventDefault();
        const er = {};
        if (!form.title.trim()) er.title = 'Give it a short title.';
        if (!form.description.trim()) er.description = 'Describe what you need.';
        if (type === 'PERMISSION') {
            if (!form.startDate) er.startDate = 'Choose a start date.';
            if (!form.endDate) er.endDate = 'Choose an end date.';
            else if (form.startDate && form.endDate < form.startDate) er.endDate = 'Must be after the start date.';
        }
        setErrors(er);
        if (Object.keys(er).length) return;

        setBusy(true);
        setFormError('');
        try {
            await onSubmit({ submissionType: type, ...form, file });
            onClose();
        } catch (err) {
            setFormError(err.message);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            as="form"
            onSubmit={submit}
            size="lg"
            icon={MessageSquarePlus}
            title="New request"
            description="The society office gets it straight away and replies here."
            dismissible={!busy}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={busy}>Cancel</Button>
                    <Button type="submit" isLoading={busy}>Send request</Button>
                </>
            )}
        >
            <div className="stack">
                {formError && <Alert tone="error">{formError}</Alert>}
                <div className="field">
                    <span className="field-label">What would you like to do?</span>
                    <div className="choice-grid choice-grid-2" role="radiogroup" aria-label="Request type">
                        {TYPES.map((t) => (
                            <button key={t.value} type="button" role="radio" aria-checked={type === t.value} className="choice choice-tall" onClick={() => setType(t.value)}>
                                <t.icon size={18} aria-hidden="true" />
                                <span><span className="choice-label">{t.label}</span><span className="choice-hint">{t.hint}</span></span>
                            </button>
                        ))}
                    </div>
                </div>

                {type === 'PERMISSION' ? (
                    <div className="form-grid">
                        <Select label="Permission for" value={form.permissionCategory} onChange={set('permissionCategory')} className="span-all">
                            {PERMISSIONS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
                        </Select>
                        <TextInput label="From" type="date" value={form.startDate} onChange={set('startDate')} error={errors.startDate} />
                        <TextInput label="Until" type="date" value={form.endDate} min={form.startDate || undefined} onChange={set('endDate')} error={errors.endDate} />
                    </div>
                ) : (
                    <Select label="Category" value={form.category} onChange={set('category')}>
                        {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
                    </Select>
                )}

                <TextInput label="Title" placeholder={type === 'COMPLAINT' ? 'e.g. Water leaking from bathroom ceiling' : 'A short summary'} value={form.title} onChange={set('title')} error={errors.title} maxLength={120} />
                <Textarea label="Details" rows={4} placeholder="Where, since when, and anything that will help." value={form.description} onChange={set('description')} error={errors.description} />
                <FileInput label="Photo or document" optional accept="image/*,.pdf" file={file} onChange={setFile} />
            </div>
        </Modal>
    );
};

export default NewRequestModal;
