import React, { useState, useEffect } from 'react';
import { AtSign, KeyRound, Phone, UserRound, UserPlus, UserCog } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput } from './ui/Field';
import { Alert } from './ui/Feedback';
import { PASSWORD_HINT, passwordPolicyError } from '../utils/passwordPolicy';

const EMPTY = { name: '', phone: '', email: '', password: '', role: 'RESIDENT', status: 'ACTIVE' };

const ResidentModal = ({ isOpen, onClose, onSave, initialData }) => {
    const isEditMode = !!initialData;
    const [formData, setFormData] = useState(EMPTY);
    const [errors, setErrors] = useState({});
    const [formError, setFormError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        setFormData(initialData ? {
            name: initialData.fullName || '',
            phone: initialData.phoneNumber || '',
            email: initialData.email || '',
            password: '', // Never prefill password
            role: 'RESIDENT',
            status: initialData.status || 'ACTIVE',
        } : EMPTY);
        setErrors({});
        setFormError('');
    }, [initialData, isOpen]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setErrors((er) => ({ ...er, [e.target.name]: undefined }));
    };

    const validate = () => {
        const e = {};
        if (formData.name.trim().length < 2) e.name = 'Enter the full name.';
        if (!/^\S+@\S+\.\S+$/.test(formData.email.trim())) e.email = 'Enter a valid email address.';
        if (!/^\d{10}$/.test(formData.phone.replace(/[\s-]/g, ''))) e.phone = 'Enter a 10-digit mobile number.';
        if (!isEditMode && formData.password) {
            const policy = passwordPolicyError(formData.password);
            if (policy) e.password = policy;
        }
        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validate()) return;
        setIsLoading(true);
        setFormError('');
        try {
            await onSave({ ...formData, name: formData.name.trim(), email: formData.email.trim(), phone: formData.phone.replace(/[\s-]/g, '') });
            onClose();
        } catch (error) {
            setFormError(error.message);
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
            icon={isEditMode ? UserCog : UserPlus}
            title={isEditMode ? 'Edit details' : 'Add a resident'}
            description={isEditMode ? 'Update this person’s contact details.' : 'They will sign in with a temporary password and set up their own account.'}
            dismissible={!isLoading}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isLoading}>Cancel</Button>
                    <Button type="submit" isLoading={isLoading}>{isEditMode ? 'Save changes' : 'Add resident'}</Button>
                </>
            )}
        >
            <div className="stack">
                {formError && <Alert tone="error">{formError}</Alert>}
                <TextInput label="Full name" name="name" icon={UserRound} value={formData.name} onChange={handleChange} error={errors.name} placeholder="Priya Sharma" />
                <div className="form-grid">
                    <TextInput label="Email" name="email" type="email" icon={AtSign} value={formData.email} onChange={handleChange} error={errors.email} placeholder="name@example.com" />
                    <TextInput label="Mobile" name="phone" type="tel" icon={Phone} value={formData.phone} onChange={handleChange} error={errors.phone} placeholder="98765 43210" />
                </div>
                {!isEditMode && (
                    <TextInput label="Temporary password" optional name="password" type="password" icon={KeyRound} value={formData.password}
                        onChange={handleChange} error={errors.password} autoComplete="new-password" placeholder="Leave blank to generate one"
                        hint={`${PASSWORD_HINT}. They must change it at first sign-in.`} />
                )}
            </div>
        </Modal>
    );
};

export default ResidentModal;
