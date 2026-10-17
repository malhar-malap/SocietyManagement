import React, { useState } from 'react';
import { X, Calendar, DollarSign, Clock } from 'lucide-react';
import './ui/shared.css';

const GenerateBillsDialog = ({ isOpen, onClose, onConfirm }) => {
    const [formData, setFormData] = useState({
        billingMonth: '',
        amount: '',
        dueDate: ''
    });
    const [isLoading, setIsLoading] = useState(false);

    if (!isOpen) return null;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            await onConfirm(formData);
            onClose();
        } catch (error) {
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '400px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title" style={{ fontSize: '1.5rem' }}>Generate Monthly Bills</h2>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="input-group-modern">
                        <label className="input-label">Billing Month</label>
                        <div className="modern-input-wrapper">
                            <Calendar size={18} className="modern-input-icon" />
                            <input
                                type="month"
                                name="billingMonth"
                                className="modern-input"
                                value={formData.billingMonth}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Maintenance Amount</label>
                        <div className="modern-input-wrapper">
                            <DollarSign size={18} className="modern-input-icon" />
                            <input
                                type="number"
                                name="amount"
                                className="modern-input"
                                value={formData.amount}
                                onChange={handleChange}
                                min="1"
                                placeholder="5000"
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group-modern">
                        <label className="input-label">Due Date</label>
                        <div className="modern-input-wrapper">
                            <Clock size={18} className="modern-input-icon" />
                            <input
                                type="date"
                                name="dueDate"
                                className="modern-input"
                                value={formData.dueDate}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    <div className="premium-actions">
                        <button type="button" className="custom-btn secondary" onClick={onClose} disabled={isLoading}>Cancel</button>
                        <button type="submit" className="custom-btn primary" disabled={isLoading}>
                            {isLoading ? <span className="spinner"></span> : 'Generate Bills'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default GenerateBillsDialog;
