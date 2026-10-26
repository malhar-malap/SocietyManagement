import React, { useState, useEffect, useCallback } from 'react';
import { CalendarCheck, History, Star, UserPlus, Users } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import StatusChip from './ui/StatusChip';
import { TextInput, Select } from './ui/Field';
import { Alert, EmptyState, Segmented, Skeleton } from './ui/Feedback';
import { formatDate } from '../utils/format';
import { getRoomOccupants, assignRoomOccupant, endRoomOccupancy, correctRoomOccupantDates, getResidents } from '../api/adminApi';

const getLocalDateString = (d = new Date()) => {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

const OccupancyModal = ({ isOpen, onClose, room, onOccupantsChanged }) => {
    const [occupants, setOccupants] = useState([]);
    const [residentsList, setResidentsList] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [showHistory, setShowHistory] = useState(false);

    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    // Add occupant form state
    const [isAddFormOpen, setIsAddFormOpen] = useState(false);
    const [selectedResidentId, setSelectedResidentId] = useState('');
    const [relationshipType, setRelationshipType] = useState('OWNER');
    const [startDate, setStartDate] = useState(getLocalDateString());
    const [isPrimaryContact, setIsPrimaryContact] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // End occupancy inline state
    const [endingOccupantId, setEndingOccupantId] = useState(null);
    const [endDate, setEndDate] = useState(getLocalDateString());
    const [isEnding, setIsEnding] = useState(false);

    // Date correction inline state (for historical records)
    const [correctingOccupantId, setCorrectingOccupantId] = useState(null);
    const [correctionStartDate, setCorrectionStartDate] = useState('');
    const [correctionEndDate, setCorrectionEndDate] = useState('');
    const [correctionReason, setCorrectionReason] = useState('');
    const [isCorrecting, setIsCorrecting] = useState(false);

    const fetchOccupantsAndResidents = useCallback(async () => {
        if (!room?.id) return;
        setIsLoading(true);
        setErrorMessage('');
        try {
            const [occupantsData, allResidents] = await Promise.all([
                getRoomOccupants(room.id),
                getResidents()
            ]);
            setOccupants(occupantsData || []);

            // Only allow assigning active users whose role is exactly RESIDENT
            const eligible = (allResidents || []).filter(u => 
                u.role === 'RESIDENT' && 
                (u.status === 'ACTIVE' || u.status === undefined) && 
                u.active !== false
            );
            setResidentsList(eligible);

            // Default primary contact to true if room currently has no active occupants
            const hasActive = (occupantsData || []).some(o => o.active);
            setIsPrimaryContact(!hasActive);
        } catch (err) {
            console.error("Failed to load occupancy data", err);
            const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to load occupants.";
            setErrorMessage(msg);
        } finally {
            setIsLoading(false);
        }
    }, [room?.id]);

    useEffect(() => {
        if (isOpen && room?.id) {
            fetchOccupantsAndResidents();
            setIsAddFormOpen(false);
            setEndingOccupantId(null);
            setErrorMessage('');
            setSuccessMessage('');
            setStartDate(getLocalDateString());
            setEndDate(getLocalDateString());
        }
    }, [isOpen, room?.id, fetchOccupantsAndResidents]);

    if (!isOpen || !room) return null;

    const activeOccupants = occupants.filter(o => o.active);
    const displayedOccupants = showHistory ? occupants : activeOccupants;

    const handleAssignOccupant = async (e) => {
        e.preventDefault();
        setErrorMessage('');
        setSuccessMessage('');

        if (!selectedResidentId) {
            setErrorMessage("Choose a resident to assign.");
            return;
        }

        setIsSubmitting(true);
        try {
            await assignRoomOccupant(room.id, {
                residentId: Number(selectedResidentId),
                relationshipType,
                startDate: startDate || null,
                primaryContact: isPrimaryContact
            });
            setSuccessMessage("Occupant assigned.");
            setIsAddFormOpen(false);
            setSelectedResidentId('');
            await fetchOccupantsAndResidents();
            if (onOccupantsChanged) onOccupantsChanged();
        } catch (err) {
            console.error("Failed to assign occupant", err);
            const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to assign occupant.";
            setErrorMessage(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleConfirmEndOccupancy = async (occupantId) => {
        setErrorMessage('');
        setSuccessMessage('');
        setIsEnding(true);
        try {
            await endRoomOccupancy(occupantId, endDate || null);
            setSuccessMessage("Occupancy ended.");
            setEndingOccupantId(null);
            await fetchOccupantsAndResidents();
            if (onOccupantsChanged) onOccupantsChanged();
        } catch (err) {
            console.error("Failed to end occupancy", err);
            const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to end occupancy.";
            setErrorMessage(msg);
        } finally {
            setIsEnding(false);
        }
    };

    const handleConfirmDateCorrection = async (occupantId) => {
        if (!correctionReason.trim()) {
            setErrorMessage("Add a reason for the correction.");
            return;
        }
        setIsCorrecting(true);
        setErrorMessage('');
        setSuccessMessage('');
        try {
            await correctRoomOccupantDates(occupantId, {
                startDate: correctionStartDate || null,
                endDate: correctionEndDate || null,
                reason: correctionReason.trim()
            });
            setSuccessMessage("Dates corrected. The change is recorded in the system log.");
            setCorrectingOccupantId(null);
            await fetchOccupantsAndResidents();
            if (onOccupantsChanged) onOccupantsChanged();
        } catch (err) {
            console.error("Failed to correct occupancy dates", err);
            const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to correct occupancy dates.";
            setErrorMessage(msg);
        } finally {
            setIsCorrecting(false);
        }
    };

    const resetMessages = () => { setErrorMessage(''); setSuccessMessage(''); };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            size="lg"
            icon={Users}
            title={`Unit ${room.roomNumber} occupants`}
            description={`${room.wing ? `Wing ${room.wing} · ` : ''}Floor ${room.floor} · owners, tenants and occupancy history`}
            footer={<Button variant="secondary" onClick={onClose}>Done</Button>}
        >
            <div className="stack">
                {errorMessage && <Alert tone="error">{errorMessage}</Alert>}
                {successMessage && <Alert tone="success">{successMessage}</Alert>}

                <div className="row row-wrap row-between">
                    <Segmented
                        label="Which occupants to show"
                        value={showHistory ? 'all' : 'current'}
                        onChange={(v) => setShowHistory(v === 'all')}
                        options={[{ value: 'current', label: `Current ${activeOccupants.length}` }, { value: 'all', label: `Full history ${occupants.length}` }]}
                    />
                    {!isAddFormOpen && (
                        <Button size="sm" icon={UserPlus} onClick={() => { setIsAddFormOpen(true); resetMessages(); }}>Add occupant</Button>
                    )}
                </div>

                {isAddFormOpen && (
                    <form onSubmit={handleAssignOccupant} className="inset-panel stack" noValidate>
                        <div className="fw-600">Assign a resident to {room.roomNumber}</div>
                        <div className="form-grid">
                            <Select label="Resident" required value={selectedResidentId} onChange={(e) => setSelectedResidentId(e.target.value)} className="span-all">
                                <option value="">Choose a resident…</option>
                                {residentsList.map((r) => (
                                    <option key={r.id} value={r.id}>{r.name || r.fullName} — {r.email}</option>
                                ))}
                            </Select>
                            <Select label="Relationship" value={relationshipType} onChange={(e) => setRelationshipType(e.target.value)}>
                                <option value="OWNER">Owner</option>
                                <option value="TENANT">Tenant</option>
                            </Select>
                            <TextInput label="Moved in on" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                        </div>
                        <label className="check">
                            <input type="checkbox" checked={isPrimaryContact} onChange={(e) => setIsPrimaryContact(e.target.checked)} />
                            Make this the unit’s primary contact
                        </label>
                        <div className="row row-end">
                            <Button variant="ghost" onClick={() => setIsAddFormOpen(false)} disabled={isSubmitting}>Cancel</Button>
                            <Button type="submit" isLoading={isSubmitting}>Assign occupant</Button>
                        </div>
                    </form>
                )}

                {isLoading ? (
                    <div className="stack stack-sm">{[0, 1].map((i) => <Skeleton key={i} height={72} style={{ borderRadius: 10 }} />)}</div>
                ) : displayedOccupants.length === 0 ? (
                    <div className="inset-panel">
                        <EmptyState compact icon={Users} title={showHistory ? 'No records yet' : 'No one lives here yet'}>
                            {showHistory ? 'Occupancy records for this unit will appear here.' : 'Add an owner or tenant to link them to this unit.'}
                        </EmptyState>
                    </div>
                ) : (
                    <ul className="occupant-list">
                        {displayedOccupants.map((occ) => (
                            <li key={occ.id} className={occ.active ? '' : 'is-ended'}>
                                <div className="occupant-row">
                                    <span className="avatar" aria-hidden="true">{(occ.residentName || '?').trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase()}</span>
                                    <div className="grow">
                                        <div className="row row-wrap" style={{ '--gap': '0.4rem' }}>
                                            <span className="fw-600">{occ.residentName || 'Unknown'}</span>
                                            <span className="tag">{occ.relationshipType === 'TENANT' ? 'Tenant' : 'Owner'}</span>
                                            {occ.primaryContact && <span className="chip chip-brand no-dot"><Star size={11} /> Primary</span>}
                                            {!occ.active && <StatusChip tone="neutral" label="Moved out" />}
                                        </div>
                                        <div className="text-sm text-muted truncate">{occ.residentEmail}{occ.residentPhone ? ` · ${occ.residentPhone}` : ''}</div>
                                        <div className="text-xs text-subtle mt-1">
                                            {occ.startDate ? `From ${formatDate(occ.startDate)}` : 'Start date not recorded'}
                                            {occ.endDate ? ` to ${formatDate(occ.endDate)}` : occ.active ? ' · present' : ''}
                                        </div>
                                    </div>
                                    {occ.active ? (
                                        <Button variant="danger-ghost" size="sm" onClick={() => { setEndingOccupantId(occ.id); setCorrectingOccupantId(null); setEndDate(getLocalDateString()); resetMessages(); }}>
                                            End occupancy
                                        </Button>
                                    ) : (
                                        <Button variant="ghost" size="sm" icon={History} onClick={() => {
                                            setCorrectingOccupantId(occ.id);
                                            setCorrectionStartDate(occ.startDate || '');
                                            setCorrectionEndDate(occ.endDate || '');
                                            setCorrectionReason('');
                                            setEndingOccupantId(null);
                                            resetMessages();
                                        }}>
                                            Correct dates
                                        </Button>
                                    )}
                                </div>

                                {endingOccupantId === occ.id && (
                                    <div className="occupant-panel tone-danger">
                                        <div className="text-sm fw-600">End {occ.residentName}’s occupancy?</div>
                                        <div className="row row-wrap" style={{ alignItems: 'flex-end' }}>
                                            <TextInput label="Moved out on" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                                            <div className="row grow row-end">
                                                <Button variant="ghost" size="sm" onClick={() => setEndingOccupantId(null)} disabled={isEnding}>Keep</Button>
                                                <Button variant="danger" size="sm" icon={CalendarCheck} onClick={() => handleConfirmEndOccupancy(occ.id)} isLoading={isEnding}>End occupancy</Button>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {correctingOccupantId === occ.id && (
                                    <div className="occupant-panel">
                                        <div className="text-sm fw-600">Correct dates for {occ.residentName}</div>
                                        <div className="form-grid">
                                            <TextInput label="Start date" type="date" value={correctionStartDate} onChange={(e) => setCorrectionStartDate(e.target.value)} />
                                            <TextInput label="End date" type="date" value={correctionEndDate} onChange={(e) => setCorrectionEndDate(e.target.value)} />
                                            <TextInput label="Reason" required className="span-all" placeholder="e.g. Move-in date checked against the lease agreement"
                                                value={correctionReason} onChange={(e) => setCorrectionReason(e.target.value)} hint="Saved to the system log with your name." />
                                        </div>
                                        <div className="row row-end">
                                            <Button variant="ghost" size="sm" onClick={() => setCorrectingOccupantId(null)} disabled={isCorrecting}>Cancel</Button>
                                            <Button size="sm" onClick={() => handleConfirmDateCorrection(occ.id)} isLoading={isCorrecting}>Save correction</Button>
                                        </div>
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </Modal>
    );
};

export default OccupancyModal;
