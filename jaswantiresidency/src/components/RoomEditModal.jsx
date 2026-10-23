import React, { useState, useEffect } from 'react';
import Button from './ui/Button';
import { Save, AlertCircle } from 'lucide-react';
import { createRoom, updateRoom } from '../api/adminApi';
import './ui/shared.css';

const RoomEditModal = ({ isOpen, onClose, room, onSaved }) => {
    const isEdit = Boolean(room?.id);

    const [roomNumber, setRoomNumber] = useState('');
    const [wing, setWing] = useState('A');
    const [floor, setFloor] = useState(1);
    const [unitCategory, setUnitCategory] = useState('RESIDENTIAL');
    const [nocApplicable, setNocApplicable] = useState(false);
    const [nocAmount, setNocAmount] = useState('0.00');
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (room) {
            setRoomNumber(room.roomNumber || '');
            setWing(room.wing || 'A');
            setFloor(room.floor || 1);
            setUnitCategory(room.unitCategory || 'RESIDENTIAL');
            setNocApplicable(Boolean(room.nocApplicable));
            setNocAmount(room.nocAmount != null ? String(room.nocAmount) : '0.00');
        } else {
            setRoomNumber('');
            setWing('A');
            setFloor(1);
            setUnitCategory('RESIDENTIAL');
            setNocApplicable(false);
            setNocAmount('0.00');
        }
        setError('');
    }, [room, isOpen]);

    const handleCategoryChange = (val) => {
        setUnitCategory(val);
        if (val !== 'RESIDENTIAL') {
            setNocApplicable(true);
            if (parseFloat(nocAmount) <= 0) {
                setNocAmount('500.00'); // suggested default NOC charge for commercial units
            }
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!roomNumber.trim()) {
            setError('Room number is required.');
            return;
        }

        setIsSaving(true);
        setError('');
        try {
            const payload = {
                roomNumber: roomNumber.trim(),
                wing: wing.trim(),
                floor: parseInt(floor, 10),
                unitCategory,
                nocApplicable,
                nocAmount: nocApplicable ? (parseFloat(nocAmount) || 0) : 0
            };

            if (isEdit) {
                await updateRoom(room.id, payload);
            } else {
                await createRoom(payload);
            }

            if (onSaved) onSaved();
            onClose();
        } catch (err) {
            setError(err?.response?.data?.message || err.message || 'Failed to save unit.');
        } finally {
            setIsSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '480px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title">{isEdit ? `Edit Room ${room.roomNumber}` : 'Add New Unit'}</h2>
                        <p className="premium-modal-subtitle">Configure unit category and automatic companion NOC billing</p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <span style={{ fontSize: '1.5rem', lineHeight: 1 }}>×</span>
                    </button>
                </div>

                <form onSubmit={handleSubmit} style={{ padding: '1.25rem 1.5rem' }}>
                    {error && (
                        <div style={{ marginBottom: '1rem', padding: '0.6rem 0.8rem', background: 'rgba(239,68,68,0.1)', color: 'var(--danger)', borderRadius: 6, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <AlertCircle size={16} />
                            <span>{error}</span>
                        </div>
                    )}

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                        <div>
                            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                Unit No <span style={{ color: 'red' }}>*</span>
                            </label>
                            <input
                                type="text"
                                required
                                value={roomNumber}
                                onChange={(e) => setRoomNumber(e.target.value)}
                                style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                            />
                        </div>
                        <div>
                            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                Wing
                            </label>
                            <input
                                type="text"
                                value={wing}
                                onChange={(e) => setWing(e.target.value)}
                                style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                            />
                        </div>
                        <div>
                            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                Floor
                            </label>
                            <input
                                type="number"
                                value={floor}
                                onChange={(e) => setFloor(e.target.value)}
                                style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                            />
                        </div>
                    </div>

                    <div style={{ marginBottom: '1rem' }}>
                        <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                            Unit Category <span style={{ color: 'red' }}>*</span>
                        </label>
                        <select
                            value={unitCategory}
                            onChange={(e) => handleCategoryChange(e.target.value)}
                            style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                        >
                            <option value="RESIDENTIAL">Residential Apartment</option>
                            <option value="COMMERCIAL">Commercial / Retail</option>
                            <option value="OFFICE">Office / Professional Suite</option>
                            <option value="SHOP">Shop / Ground Floor Store</option>
                        </select>
                    </div>

                    <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: 8, marginBottom: '1.25rem', border: '1px solid var(--border-light)' }}>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontWeight: 600, fontSize: '0.85rem' }}>
                            <input
                                type="checkbox"
                                checked={nocApplicable}
                                onChange={(e) => setNocApplicable(e.target.checked)}
                            />
                            Apply Automatic Companion NOC Charge
                        </label>
                        <p style={{ margin: '0.35rem 0 0.75rem 1.4rem', fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>
                            When active, monthly billing generation will automatically generate a separate companion NOC invoice.
                        </p>

                        {nocApplicable && (
                            <div style={{ marginLeft: '1.4rem' }}>
                                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                    Monthly NOC Amount (₹)
                                </label>
                                <input
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    value={nocAmount}
                                    onChange={(e) => setNocAmount(e.target.value)}
                                    style={{ width: '160px', padding: '0.45rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                                />
                            </div>
                        )}
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        <Button variant="secondary" type="button" onClick={onClose}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={isSaving}>
                            <Save size={16} /> {isSaving ? 'Saving...' : 'Save Unit'}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default RoomEditModal;
