import React, { useState, useEffect } from 'react';
import { X, Type, FileText, Calendar } from 'lucide-react';
import './ui/shared.css';

const AnnouncementModal = ({ isOpen, onClose, onSave, initialData }) => {
    const isEditMode = !!initialData;
    
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        expiryDate: ''
    });
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (initialData) {
            setFormData({
                title: initialData.title || '',
                description: initialData.description || '',
                expiryDate: initialData.expiryDate ? initialData.expiryDate.split('T')[0] : ''
            });
        } else {
            setFormData({ title: '', description: '', expiryDate: '' });
        }
    }, [initialData, isOpen]);

    if (!isOpen) return null;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            // Convert date to ISO format if provided
            let payload = { ...formData };
            if (payload.expiryDate) {
                payload.expiryDate = `${payload.expiryDate}T23:59:59`;
            } else {
                payload.expiryDate = null;
            }
            await onSave(payload);
            onClose();
        } catch (error) {
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content">
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title">
                            {isEditMode ? 'Edit Announcement' : 'Publish Announcement'}
                        </h2>
                        <p className="premium-modal-subtitle">
                            {isEditMode ? 'Update the announcement details below.' : 'Create a new announcement for the residents.'}
                        </p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose} aria-label="Close modal">
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    
                    <div className="input-group-modern">
                        <label className="input-label">Title</label>
                        <div className="modern-input-wrapper">
                            <Type size={18} className="modern-input-icon" />
                            <input
                                type="text"
                                name="title"
                                className="modern-input"
                                value={formData.title}
                                onChange={handleChange}
                                placeholder="Water supply maintenance"
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Description</label>
                        <div className="modern-input-wrapper" style={{ alignItems: 'flex-start' }}>
                            <FileText size={18} className="modern-input-icon" style={{ top: '1rem' }} />
                            <textarea
                                name="description"
                                className="modern-input"
                                value={formData.description}
                                onChange={handleChange}
                                placeholder="Provide detailed information here..."
                                rows="4"
                                style={{ height: 'auto', paddingTop: '1rem' }}
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Expiry Date (Optional)</label>
                        <div className="modern-input-wrapper">
                            <Calendar size={18} className="modern-input-icon" />
                            <input
                                type="date"
                                name="expiryDate"
                                className="modern-input"
                                value={formData.expiryDate}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div className="premium-actions">
                        <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>Cancel</button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading ? <span className="spinner"></span> : (isEditMode ? 'Save Changes' : 'Publish')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default AnnouncementModal;
