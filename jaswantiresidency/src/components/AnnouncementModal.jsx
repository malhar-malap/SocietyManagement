import React, { useState, useEffect } from 'react';
import { Megaphone } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput, Textarea } from './ui/Field';
import { Alert } from './ui/Feedback';

const AnnouncementModal = ({ isOpen, onClose, onSave, initialData }) => {
    const isEditMode = !!initialData;
    const [formData, setFormData] = useState({ title: '', description: '', expiryDate: '' });
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        setFormData(initialData ? {
            title: initialData.title || '',
            description: initialData.description || '',
            expiryDate: initialData.expiryDate ? initialData.expiryDate.split('T')[0] : '',
        } : { title: '', description: '', expiryDate: '' });
        setError('');
    }, [initialData, isOpen]);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.title.trim() || !formData.description.trim()) {
            setError('Add a title and the message residents should read.');
            return;
        }
        setIsLoading(true);
        try {
            await onSave({
                title: formData.title.trim(),
                description: formData.description.trim(),
                expiryDate: formData.expiryDate ? `${formData.expiryDate}T23:59:59` : null,
            });
            onClose();
        } catch (err) {
            setError(err.message);
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
            icon={Megaphone}
            title={isEditMode ? 'Edit notice' : 'New notice'}
            description={isEditMode ? 'Changes show for residents straight away.' : 'Every resident sees this on their home screen and gets a notification.'}
            dismissible={!isLoading}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isLoading}>Cancel</Button>
                    <Button type="submit" isLoading={isLoading}>{isEditMode ? 'Save changes' : 'Publish notice'}</Button>
                </>
            )}
        >
            <div className="stack">
                {error && <Alert tone="error">{error}</Alert>}
                <TextInput label="Title" name="title" value={formData.title} onChange={handleChange} placeholder="Water supply shutdown on Saturday" maxLength={120} />
                <Textarea label="Message" name="description" rows={5} value={formData.description} onChange={handleChange}
                    placeholder="What is happening, when, and what residents should do." />
                <TextInput label="Show until" optional type="date" name="expiryDate" value={formData.expiryDate} onChange={handleChange}
                    hint="After this date the notice moves to Expired." />
            </div>
        </Modal>
    );
};

export default AnnouncementModal;
