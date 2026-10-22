import React, { useState, useEffect, useCallback } from 'react';
import { X, UserPlus, UserCheck, AlertCircle, CheckCircle } from 'lucide-react';
import Button from './ui/Button';
import { getRoomOccupants, assignRoomOccupant, endRoomOccupancy, correctRoomOccupantDates, getResidents } from '../api/adminApi';
import './ui/shared.css';

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
            setErrorMessage("Please select a resident to assign.");
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
            setSuccessMessage("Occupant assigned successfully.");
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
            setSuccessMessage("Occupancy ended successfully.");
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
            setErrorMessage("Correction reason is required and cannot be blank.");
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
            setSuccessMessage("Historical occupancy dates corrected and audited successfully.");
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

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '850px', width: '95%' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title">
                            Unit {room.roomNumber} Occupancy Registry
                        </h2>
                        <p className="premium-modal-subtitle">
                            {room.wing ? `Wing ${room.wing}, ` : ''}Floor {room.floor} • Manage current owners, tenants, and occupancy history.
                        </p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose} aria-label="Close modal">
                        <X size={20} />
                    </button>
                </div>

                {errorMessage && (
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.75rem',
                        padding: '0.875rem 1rem',
                        backgroundColor: 'rgba(239, 68, 68, 0.1)',
                        border: '1px solid rgba(239, 68, 68, 0.25)',
                        borderRadius: '8px',
                        color: 'var(--danger, #ef4444)',
                        marginBottom: '1rem',
                        fontSize: '0.9rem'
                    }}>
                        <AlertCircle size={18} />
                        <span>{errorMessage}</span>
                    </div>
                )}

                {successMessage && (
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.75rem',
                        padding: '0.875rem 1rem',
                        backgroundColor: 'rgba(16, 185, 129, 0.1)',
                        border: '1px solid rgba(16, 185, 129, 0.25)',
                        borderRadius: '8px',
                        color: 'var(--success, #10b981)',
                        marginBottom: '1rem',
                        fontSize: '0.9rem'
                    }}>
                        <CheckCircle size={18} />
                        <span>{successMessage}</span>
                    </div>
                )}

                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '1rem',
                    flexWrap: 'wrap',
                    gap: '0.75rem'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary-light)' }}>
                            Active Occupants: {activeOccupants.length}
                        </span>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                checked={showHistory}
                                onChange={(e) => setShowHistory(e.target.checked)}
                            />
                            Show Inactive / Full History
                        </label>
                    </div>
                    <Button
                        variant="primary"
                        onClick={() => {
                            setIsAddFormOpen(!isAddFormOpen);
                            setErrorMessage('');
                            setSuccessMessage('');
                        }}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.45rem 0.9rem', fontSize: '0.85rem' }}
                    >
                        <UserPlus size={16} />
                        {isAddFormOpen ? 'Close Form' : 'Add Occupant'}
                    </Button>
                </div>

                {isAddFormOpen && (
                    <form onSubmit={handleAssignOccupant} style={{
                        background: 'var(--background-light, #f9fafb)',
                        border: '1px solid var(--border-light, #e5e7eb)',
                        borderRadius: '12px',
                        padding: '1.25rem',
                        marginBottom: '1.5rem'
                    }}>
                        <h4 style={{ margin: '0 0 1rem 0', fontSize: '0.95rem', fontWeight: 600 }}>Assign Resident to Unit {room.roomNumber}</h4>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
                            <div>
                                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.35rem' }}>Select Resident *</label>
                                <select
                                    value={selectedResidentId}
                                    onChange={(e) => setSelectedResidentId(e.target.value)}
                                    required
                                    style={{
                                        width: '100%',
                                        padding: '0.5rem',
                                        borderRadius: '6px',
                                        border: '1px solid var(--border-light, #d1d5db)',
                                        fontSize: '0.9rem'
                                    }}
                                >
                                    <option value="">-- Choose Resident --</option>
                                    {residentsList.map(r => (
                                        <option key={r.id} value={r.id}>
                                            {r.name || r.fullName} ({r.email})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.35rem' }}>Relationship Type *</label>
                                <select
                                    value={relationshipType}
                                    onChange={(e) => setRelationshipType(e.target.value)}
                                    required
                                    style={{
                                        width: '100%',
                                        padding: '0.5rem',
                                        borderRadius: '6px',
                                        border: '1px solid var(--border-light, #d1d5db)',
                                        fontSize: '0.9rem'
                                    }}
                                >
                                    <option value="OWNER">Owner</option>
                                    <option value="TENANT">Tenant</option>
                                </select>
                            </div>

                            <div>
                                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.35rem' }}>Start Date</label>
                                <input
                                    type="date"
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                    style={{
                                        width: '100%',
                                        padding: '0.5rem',
                                        borderRadius: '6px',
                                        border: '1px solid var(--border-light, #d1d5db)',
                                        fontSize: '0.9rem'
                                    }}
                                />
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', cursor: 'pointer' }}>
                                <input
                                    type="checkbox"
                                    checked={isPrimaryContact}
                                    onChange={(e) => setIsPrimaryContact(e.target.checked)}
                                />
                                <span style={{ fontWeight: 500 }}>Set as Primary Unit Contact</span>
                            </label>

                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                <Button type="button" variant="secondary" onClick={() => setIsAddFormOpen(false)} disabled={isSubmitting}>
                                    Cancel
                                </Button>
                                <Button type="submit" variant="primary" isLoading={isSubmitting}>
                                    Assign Occupant
                                </Button>
                            </div>
                        </div>
                    </form>
                )}

                {/* Occupants Table */}
                <div style={{ overflowX: 'auto', border: '1px solid var(--border-light, #e5e7eb)', borderRadius: '10px' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                        <thead>
                            <tr style={{ background: 'var(--background-light, #f9fafb)', borderBottom: '1px solid var(--border-light, #e5e7eb)', textAlign: 'left' }}>
                                <th style={{ padding: '0.75rem 1rem' }}>Resident</th>
                                <th style={{ padding: '0.75rem 1rem' }}>Type</th>
                                <th style={{ padding: '0.75rem 1rem' }}>Primary</th>
                                <th style={{ padding: '0.75rem 1rem' }}>Dates</th>
                                <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                                <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading ? (
                                <tr>
                                    <td colSpan="6" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary-light)' }}>
                                        Loading occupants...
                                    </td>
                                </tr>
                            ) : displayedOccupants.length === 0 ? (
                                <tr>
                                    <td colSpan="6" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary-light)' }}>
                                        {showHistory ? "No occupancy records found for this unit." : "No active occupants. Check 'Show Inactive / Full History' or assign an occupant."}
                                    </td>
                                </tr>
                            ) : (
                                displayedOccupants.map(occ => (
                                    <React.Fragment key={occ.id}>
                                        <tr style={{ borderBottom: '1px solid var(--border-light, #e5e7eb)' }}>
                                            <td style={{ padding: '0.75rem 1rem' }}>
                                                <div style={{ fontWeight: 600 }}>{occ.residentName || 'Unknown'}</div>
                                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)' }}>
                                                    {occ.residentEmail} {occ.residentPhone ? `• ${occ.residentPhone}` : ''}
                                                </div>
                                            </td>
                                            <td style={{ padding: '0.75rem 1rem' }}>
                                                <span style={{
                                                    fontSize: '0.75rem',
                                                    fontWeight: 600,
                                                    padding: '0.2rem 0.5rem',
                                                    borderRadius: '4px',
                                                    backgroundColor: occ.relationshipType === 'OWNER' ? 'rgba(59, 130, 246, 0.1)' : 'rgba(245, 158, 11, 0.1)',
                                                    color: occ.relationshipType === 'OWNER' ? '#2563eb' : '#d97706'
                                                }}>
                                                    {occ.relationshipType}
                                                </span>
                                            </td>
                                            <td style={{ padding: '0.75rem 1rem' }}>
                                                {occ.primaryContact ? (
                                                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--success, #10b981)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                                                        <UserCheck size={14} /> Yes
                                                    </span>
                                                ) : (
                                                    <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>No</span>
                                                )}
                                            </td>
                                            <td style={{ padding: '0.75rem 1rem', fontSize: '0.8rem' }}>
                                                <div>Start: {occ.startDate || 'Not recorded'}</div>
                                                <div>End: {occ.endDate || (occ.active ? 'Present' : 'Ended')}</div>
                                            </td>
                                            <td style={{ padding: '0.75rem 1rem' }}>
                                                <span style={{
                                                    fontSize: '0.75rem',
                                                    fontWeight: 600,
                                                    padding: '0.2rem 0.5rem',
                                                    borderRadius: '4px',
                                                    backgroundColor: occ.active ? 'rgba(16, 185, 129, 0.1)' : 'rgba(107, 114, 128, 0.1)',
                                                    color: occ.active ? '#10b981' : '#6b7280'
                                                }}>
                                                    {occ.active ? 'ACTIVE' : 'ENDED'}
                                                </span>
                                            </td>
                                            <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                                                {occ.active ? (
                                                    <Button
                                                        variant="danger"
                                                        size="small"
                                                        onClick={() => {
                                                            setEndingOccupantId(occ.id);
                                                            setEndDate(getLocalDateString());
                                                            setErrorMessage('');
                                                            setSuccessMessage('');
                                                        }}
                                                        style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
                                                    >
                                                        End Occupancy
                                                    </Button>
                                                ) : (
                                                    <Button
                                                        variant="secondary"
                                                        size="small"
                                                        onClick={() => {
                                                            setCorrectingOccupantId(occ.id);
                                                            setCorrectionStartDate(occ.startDate || '');
                                                            setCorrectionEndDate(occ.endDate || '');
                                                            setCorrectionReason('');
                                                            setEndingOccupantId(null);
                                                            setErrorMessage('');
                                                            setSuccessMessage('');
                                                        }}
                                                        style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
                                                    >
                                                        Correct Dates
                                                    </Button>
                                                )}
                                            </td>
                                        </tr>

                                        {endingOccupantId === occ.id && (
                                            <tr style={{ background: 'rgba(239, 68, 68, 0.05)', borderBottom: '1px solid var(--border-light, #e5e7eb)' }}>
                                                <td colSpan="6" style={{ padding: '0.875rem 1rem' }}>
                                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
                                                        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--danger, #ef4444)' }}>
                                                            End occupancy for {occ.residentName}?
                                                        </span>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                            <label style={{ fontSize: '0.8rem', fontWeight: 500 }}>End Date:</label>
                                                            <input
                                                                type="date"
                                                                value={endDate}
                                                                onChange={(e) => setEndDate(e.target.value)}
                                                                style={{
                                                                    padding: '0.35rem 0.5rem',
                                                                    borderRadius: '6px',
                                                                    border: '1px solid var(--border-light, #d1d5db)',
                                                                    fontSize: '0.85rem'
                                                                }}
                                                            />
                                                            <Button
                                                                variant="danger"
                                                                onClick={() => handleConfirmEndOccupancy(occ.id)}
                                                                isLoading={isEnding}
                                                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                                                            >
                                                                Confirm End
                                                            </Button>
                                                            <Button
                                                                variant="secondary"
                                                                onClick={() => setEndingOccupantId(null)}
                                                                disabled={isEnding}
                                                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                                                            >
                                                                Cancel
                                                            </Button>
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}

                                        {correctingOccupantId === occ.id && (
                                            <tr style={{ background: 'rgba(59, 130, 246, 0.05)', borderBottom: '1px solid var(--border-light, #e5e7eb)' }}>
                                                <td colSpan="6" style={{ padding: '1rem' }}>
                                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                                                            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary-light, #1f2937)' }}>
                                                                Correct Historical Dates for {occ.residentName} (Audited)
                                                            </span>
                                                            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>
                                                                Updates will be saved to the activity audit trail.
                                                            </span>
                                                        </div>
                                                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem' }}>
                                                            <div>
                                                                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>Start Date</label>
                                                                <input
                                                                    type="date"
                                                                    value={correctionStartDate}
                                                                    onChange={(e) => setCorrectionStartDate(e.target.value)}
                                                                    style={{
                                                                        width: '100%',
                                                                        padding: '0.35rem 0.5rem',
                                                                        borderRadius: '6px',
                                                                        border: '1px solid var(--border-light, #d1d5db)',
                                                                        fontSize: '0.85rem'
                                                                    }}
                                                                />
                                                            </div>
                                                            <div>
                                                                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>End Date</label>
                                                                <input
                                                                    type="date"
                                                                    value={correctionEndDate}
                                                                    onChange={(e) => setCorrectionEndDate(e.target.value)}
                                                                    style={{
                                                                        width: '100%',
                                                                        padding: '0.35rem 0.5rem',
                                                                        borderRadius: '6px',
                                                                        border: '1px solid var(--border-light, #d1d5db)',
                                                                        fontSize: '0.85rem'
                                                                    }}
                                                                />
                                                            </div>
                                                            <div style={{ gridColumn: '1 / -1' }}>
                                                                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.25rem' }}>Reason for Correction *</label>
                                                                <input
                                                                    type="text"
                                                                    placeholder="e.g. Corrected move-in date verified against physical lease agreement"
                                                                    value={correctionReason}
                                                                    onChange={(e) => setCorrectionReason(e.target.value)}
                                                                    required
                                                                    style={{
                                                                        width: '100%',
                                                                        padding: '0.4rem 0.6rem',
                                                                        borderRadius: '6px',
                                                                        border: '1px solid var(--border-light, #d1d5db)',
                                                                        fontSize: '0.85rem'
                                                                    }}
                                                                />
                                                            </div>
                                                        </div>
                                                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.25rem' }}>
                                                            <Button
                                                                type="button"
                                                                variant="secondary"
                                                                onClick={() => setCorrectingOccupantId(null)}
                                                                disabled={isCorrecting}
                                                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                                                            >
                                                                Cancel
                                                            </Button>
                                                            <Button
                                                                type="button"
                                                                variant="primary"
                                                                onClick={() => handleConfirmDateCorrection(occ.id)}
                                                                isLoading={isCorrecting}
                                                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                                                            >
                                                                Save Correction
                                                            </Button>
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="premium-actions" style={{ marginTop: '1.5rem' }}>
                    <Button variant="secondary" onClick={onClose}>
                        Close
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default OccupancyModal;
