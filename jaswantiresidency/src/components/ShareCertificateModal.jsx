import React, { useState, useEffect, useCallback } from 'react';
import { Award, Download } from 'lucide-react';
import Modal from './ui/Modal';
import Button from './ui/Button';
import { TextInput, FileInput } from './ui/Field';
import { Alert, Skeleton } from './ui/Feedback';
import { formatDate } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { getShareCertificate, uploadShareCertificate } from '../api/adminApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';

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
                        setError('We could not load the certificate details.');
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
            setError('Enter the certificate number.');
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
            setSuccessMessage('Certificate saved.');
            if (onCertificateUpdated) onCertificateUpdated();
        } catch (err) {
            setError(err?.response?.data?.message || 'We could not save the certificate. Please try again.');
        } finally {
            setIsUploading(false);
        }
    };

    const hasCert = Boolean(certData?.shareCertificateNumber);

    return (
        <Modal
            isOpen={isOpen && Boolean(room)}
            onClose={onClose}
            as={isMasterAdmin ? 'form' : 'div'}
            onSubmit={isMasterAdmin ? handleUpload : undefined}
            icon={Award}
            title={`Share certificate · ${room?.roomNumber || ''}`}
            description="The society’s share certificate on record for this unit."
            dismissible={!isUploading}
            footer={isMasterAdmin ? (
                <>
                    <Button variant="secondary" onClick={onClose} disabled={isUploading}>Close</Button>
                    <Button type="submit" isLoading={isUploading} disabled={isLoading}>{hasCert ? 'Save changes' : 'Save certificate'}</Button>
                </>
            ) : <Button variant="secondary" onClick={onClose}>Close</Button>}
        >
            {isLoading ? (
                <div className="stack stack-sm"><Skeleton height={18} width="60%" /><Skeleton height={18} width="40%" /><Skeleton height={18} width="70%" /></div>
            ) : (
                <div className="stack">
                    {error && <Alert tone="error">{error}</Alert>}
                    {successMessage && <Alert tone="success">{successMessage}</Alert>}

                    <dl className="kv summary-box">
                        <dt>Certificate number</dt><dd>{certData?.shareCertificateNumber || <span className="text-subtle">Not issued</span>}</dd>
                        <dt>Issued on</dt><dd>{certData?.shareCertificateIssuedDate ? formatDate(certData.shareCertificateIssuedDate) : '—'}</dd>
                        <dt>Document</dt>
                        <dd>
                            {certData?.hasDocument ? (
                                <button type="button" className="btn btn-link btn-sm" onClick={() => downloadAuthenticatedFile(`/rooms/${room?.id}/share-certificate/download`, `share_certificate_${room?.roomNumber}.pdf`)}>
                                    <Download size={14} /> Download
                                </button>
                            ) : <span className="text-subtle">None attached</span>}
                        </dd>
                    </dl>

                    {isMasterAdmin ? (
                        <>
                            <div className="fw-600">{hasCert ? 'Update certificate' : 'Record a certificate'}</div>
                            <div className="form-grid">
                                <TextInput label="Certificate number" required placeholder="SC-JR-2026-0101" value={certNumber} onChange={(e) => setCertNumber(e.target.value)} />
                                <TextInput label="Issued on" type="date" value={issuedDate} onChange={(e) => setIssuedDate(e.target.value)} />
                            </div>
                            <FileInput label="Scanned copy" optional accept=".pdf,image/*" file={file} onChange={setFile} hint="PDF or image" />
                        </>
                    ) : (
                        <p className="text-sm text-muted">Only the master administrator can record or change share certificates.</p>
                    )}
                </div>
            )}
        </Modal>
    );
};

export default ShareCertificateModal;
