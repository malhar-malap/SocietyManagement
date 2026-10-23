import React, { useState, useEffect, useCallback } from 'react';
import Button from './ui/Button';
import { Download, Upload, CheckCircle2, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getShareCertificate, uploadShareCertificate } from '../api/adminApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';
import './ui/shared.css';

const ShareCertificateModal = ({ isOpen, onClose, room, onCertificateUpdated }) => {
    const { user } = useAuth();
    const isMasterAdmin = user?.role === 'MASTER_ADMIN';

    const [certData, setCertData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [certNumber, setCertNumber] = useState('');
    const [issuedDate, setIssuedDate] = useState('');
    const [file, setFile] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const loadCert = useCallback(async () => {
        if (!room?.id) return;
        setIsLoading(true);
        setError('');
        try {
            const data = await getShareCertificate(room.id);
            setCertData(data);
            setCertNumber(data.shareCertificateNumber || '');
            setIssuedDate(data.shareCertificateIssuedDate || '');
        } catch (err) {
            console.error('Failed to load share certificate', err);
            setError('Failed to load share certificate details.');
        } finally {
            setIsLoading(false);
        }
    }, [room?.id]);

    useEffect(() => {
        if (isOpen && room) {
            loadCert();
            setSuccessMessage('');
            setFile(null);
        }
    }, [isOpen, room, loadCert]);

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!certNumber.trim()) {
            setError('Share certificate number is required.');
            return;
        }

        setIsUploading(true);
        setError('');
        setSuccessMessage('');

        try {
            const formData = new FormData();
            formData.append('certificateNumber', certNumber.trim());
            if (issuedDate) formData.append('issuedDate', issuedDate);
            if (file) formData.append('file', file);

            const updated = await uploadShareCertificate(room.id, formData);
            setCertData(updated);
            setSuccessMessage('Share certificate successfully saved.');
            if (onCertificateUpdated) onCertificateUpdated();
        } catch (err) {
            setError(err?.response?.data?.message || err.message || 'Upload failed.');
        } finally {
            setIsUploading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="premium-modal-overlay">
            <div className="premium-modal-content" style={{ maxWidth: '520px' }}>
                <div className="premium-modal-header">
                    <div>
                        <h2 className="premium-modal-title">Share Certificate - Room {room?.roomNumber}</h2>
                        <p className="premium-modal-subtitle">Official society share certificate archiving</p>
                    </div>
                    <button className="premium-modal-close" onClick={onClose}>
                        <span style={{ fontSize: '1.5rem', lineHeight: 1 }}>×</span>
                    </button>
                </div>

                <div style={{ padding: '1.25rem 1.5rem' }}>
                    {isLoading ? (
                        <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-secondary-light)' }}>
                            Loading certificate details...
                        </div>
                    ) : (
                        <>
                            {error && (
                                <div style={{ marginBottom: '1rem', padding: '0.6rem 0.8rem', background: 'rgba(239,68,68,0.1)', color: 'var(--danger)', borderRadius: 6, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <AlertCircle size={16} />
                                    <span>{error}</span>
                                </div>
                            )}
                            {successMessage && (
                                <div style={{ marginBottom: '1rem', padding: '0.6rem 0.8rem', background: 'rgba(16,185,129,0.12)', color: 'var(--success)', borderRadius: 6, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <CheckCircle2 size={16} />
                                    <span>{successMessage}</span>
                                </div>
                            )}

                            {/* Current Certificate Status */}
                            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: 8, marginBottom: '1.5rem', fontSize: '0.875rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                                    <span style={{ color: 'var(--text-secondary-light)' }}>Certificate Number:</span>
                                    <strong>{certData?.shareCertificateNumber || 'Not Issued'}</strong>
                                </div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                                    <span style={{ color: 'var(--text-secondary-light)' }}>Issued Date:</span>
                                    <strong>{certData?.shareCertificateIssuedDate || 'N/A'}</strong>
                                </div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem', paddingTop: '0.75rem', borderTop: '1px solid #e2e8f0' }}>
                                    <span style={{ color: 'var(--text-secondary-light)' }}>Archived Document:</span>
                                    {certData?.hasDocument ? (
                                        <button
                                            type="button"
                                            onClick={() => downloadAuthenticatedFile(`/rooms/${room?.id}/share-certificate/download`, `share_certificate_${room?.roomNumber}.pdf`)}
                                            style={{
                                                background: 'none',
                                                border: 'none',
                                                cursor: 'pointer',
                                                padding: 0,
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                gap: '0.4rem',
                                                color: 'var(--primary)',
                                                fontWeight: 600,
                                                fontSize: '0.85rem'
                                            }}
                                        >
                                            <Download size={15} /> Download Document
                                        </button>
                                    ) : (
                                        <span style={{ color: 'var(--text-secondary-light)', fontStyle: 'italic' }}>No document file attached</span>
                                    )}
                                </div>
                            </div>

                            {/* Upload / Update Form (Master Admin Only) */}
                            {isMasterAdmin ? (
                                <form onSubmit={handleUpload}>
                                    <h4 style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>
                                        {certData?.shareCertificateNumber ? 'Update Share Certificate' : 'Archive New Share Certificate'}
                                    </h4>

                                    <div style={{ marginBottom: '1rem' }}>
                                        <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                            Certificate Number <span style={{ color: 'red' }}>*</span>
                                        </label>
                                        <input
                                            type="text"
                                            required
                                            placeholder="e.g. SC-JR-2026-0101"
                                            value={certNumber}
                                            onChange={(e) => setCertNumber(e.target.value)}
                                            style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                                        />
                                    </div>

                                    <div style={{ marginBottom: '1rem' }}>
                                        <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                            Issued Date
                                        </label>
                                        <input
                                            type="date"
                                            value={issuedDate}
                                            onChange={(e) => setIssuedDate(e.target.value)}
                                            style={{ width: '100%', padding: '0.5rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem' }}
                                        />
                                    </div>

                                    <div style={{ marginBottom: '1.25rem' }}>
                                        <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                                            Upload Document (PDF or Scanned Image)
                                        </label>
                                        <input
                                            type="file"
                                            accept=".pdf,image/*"
                                            onChange={(e) => setFile(e.target.files[0] || null)}
                                            style={{ width: '100%', fontSize: '0.85rem' }}
                                        />
                                    </div>

                                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                                        <Button variant="secondary" onClick={onClose} type="button">
                                            Close
                                        </Button>
                                        <Button type="submit" disabled={isUploading}>
                                            <Upload size={16} /> {isUploading ? 'Saving...' : 'Save Certificate'}
                                        </Button>
                                    </div>
                                </form>
                            ) : (
                                <div style={{ textAlign: 'right' }}>
                                    <Button variant="secondary" onClick={onClose}>Close</Button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ShareCertificateModal;
