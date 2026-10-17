import React, { useState, useEffect } from 'react';
import { X, User, Phone, Mail, Hash } from 'lucide-react';
import './ui/shared.css';

const ResidentModal = ({ isOpen, onClose, onSave, initialData }) => {
    const isEditMode = !!initialData;
    
    const [formData, setFormData] = useState({
        name: '',
        phone: '',
        email: '',
        password: '',
        role: 'RESIDENT',
        status: 'ACTIVE'
    });
    
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (initialData) {
            setFormData({
                name: initialData.fullName || '',
                phone: initialData.phoneNumber || '',
                email: initialData.email || '',
                password: '', // Never prefill password
                role: 'RESIDENT',
                status: initialData.status || 'ACTIVE'
            });
        } else {
            setFormData({ name: '', phone: '', email: '', password: '', role: 'RESIDENT', status: 'ACTIVE' });
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
            await onSave(formData);
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
                            {isEditMode ? 'Edit Resident' : 'Add New Resident'}
                        </h2>
                        <p className="premium-modal-subtitle">
                            {isEditMode ? 'Update the details of the resident below.' : 'Enter the details to register a new resident.'}
                        </p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose} aria-label="Close modal">
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    
                    <div className="input-group-modern">
                        <label className="input-label">Full Name</label>
                        <div className="modern-input-wrapper">
                            <User size={18} className="modern-input-icon" />
                            <input
                                type="text"
                                name="name"
                                className="modern-input"
                                value={formData.name}
                                onChange={handleChange}
                                placeholder="Enter full name"
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Email Address</label>
                        <div className="modern-input-wrapper">
                            <Mail size={18} className="modern-input-icon" />
                            <input
                                type="email"
                                name="email"
                                className="modern-input"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="name@example.com"
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Phone Number</label>
                        <div className="modern-input-wrapper">
                            <Phone size={18} className="modern-input-icon" />
                            <input
                                type="tel"
                                name="phone"
                                className="modern-input"
                                value={formData.phone}
                                onChange={handleChange}
                                placeholder="10-digit mobile number"
                                required
                            />
                        </div>
                    </div>

                    {!isEditMode && (
                        <div className="input-group-modern">
                            <label className="input-label">Temporary Password</label>
                            <div className="modern-input-wrapper">
                                <Hash size={18} className="modern-input-icon" />
                                <input
                                    type="password"
                                    name="password"
                                    className="modern-input"
                                    value={formData.password}
                                    onChange={handleChange}
                                    placeholder="Set initial password"
                                    required
                                />
                            </div>
                        </div>
                    )}

                    <div className="premium-actions">
                        <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>
                            Cancel
                        </button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading ? <span className="spinner"></span> : (isEditMode ? 'Save Changes' : 'Add Resident')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ResidentModal;
