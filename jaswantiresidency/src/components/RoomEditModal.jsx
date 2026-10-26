import React, { useState, useEffect } from 'react';
import { Building2 } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput, Select } from './ui/Field';
import { Alert } from './ui/Feedback';
import { createRoom, updateRoom } from '../api/adminApi';
import { notifySuccess } from '../utils/notify';

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
            setError('Enter the unit number.');
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
                nocAmount: nocApplicable ? (parseFloat(nocAmount) || 0) : 0,
            };

            if (isEdit) await updateRoom(room.id, payload);
            else await createRoom(payload);

            notifySuccess(isEdit ? `Unit ${payload.roomNumber} updated` : `Unit ${payload.roomNumber} added`);
            if (onSaved) onSaved();
            onClose();
        } catch (err) {
            setError(err?.response?.data?.message || 'We could not save this unit. Please try again.');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            as="form"
            onSubmit={handleSubmit}
            icon={Building2}
            title={isEdit ? `Edit unit ${room.roomNumber}` : 'Add a unit'}
            description="Set the unit’s location, type and any NOC charge billed with maintenance."
            dismissible={!isSaving}
            footer={(
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isSaving}>Cancel</Button>
                    <Button type="submit" isLoading={isSaving}>{isEdit ? 'Save changes' : 'Add unit'}</Button>
                </>
            )}
        >
            <div className="stack">
                {error && <Alert tone="error">{error}</Alert>}
                <div className="grid grid-3" style={{ '--gap': '0.75rem' }}>
                    <TextInput label="Unit number" required value={roomNumber} onChange={(e) => setRoomNumber(e.target.value)} placeholder="C-602" />
                    <TextInput label="Wing" value={wing} onChange={(e) => setWing(e.target.value)} />
                    <TextInput label="Floor" type="number" value={floor} onChange={(e) => setFloor(e.target.value)} />
                </div>
                <Select label="Unit type" value={unitCategory} onChange={(e) => handleCategoryChange(e.target.value)}>
                    <option value="RESIDENTIAL">Residential apartment</option>
                    <option value="COMMERCIAL">Commercial / retail</option>
                    <option value="OFFICE">Office / professional suite</option>
                    <option value="SHOP">Shop / ground-floor store</option>
                </Select>
                <div className="summary-box stack stack-sm">
                    <label className="check fw-600">
                        <input type="checkbox" checked={nocApplicable} onChange={(e) => setNocApplicable(e.target.checked)} />
                        Bill a monthly NOC charge
                    </label>
                    <p className="text-sm text-muted">When on, monthly bill generation also creates a separate NOC bill for this unit.</p>
                    {nocApplicable && (
                        <TextInput label="Monthly NOC amount" prefix="₹" type="number" step="0.01" min="0" value={nocAmount}
                            onChange={(e) => setNocAmount(e.target.value)} className="mt-2" style={{ maxWidth: 160 }} />
                    )}
                </div>
            </div>
        </Modal>
    );
};

export default RoomEditModal;
