import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import { Users, Plus, Edit2, Award, Building2 } from 'lucide-react';
import { getRooms } from '../api/adminApi';
import OccupancyModal from '../components/OccupancyModal';
import RoomEditModal from '../components/RoomEditModal';
import ShareCertificateModal from '../components/ShareCertificateModal';
import { useAuth } from '../context/AuthContext';

const AdminRooms = () => {
    const { user } = useAuth();
    const isAdminOrMaster = user?.role === 'ADMIN' || user?.role === 'MASTER_ADMIN';
    const isMasterAdmin = user?.role === 'MASTER_ADMIN';

    const [rooms, setRooms] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const [selectedRoom, setSelectedRoom] = useState(null);
    const [isOccupancyModalOpen, setIsOccupancyModalOpen] = useState(false);
    const [isRoomEditModalOpen, setIsRoomEditModalOpen] = useState(false);
    const [isCertModalOpen, setIsCertModalOpen] = useState(false);

    const fetchData = async () => {
        setIsLoading(true);
        try {
            const roomsData = await getRooms();
            setRooms(roomsData || []);
        } catch (err) {
            console.error("Failed to fetch rooms data", err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleOpenOccupancy = (room) => {
        setSelectedRoom(room);
        setIsOccupancyModalOpen(true);
    };

    const handleOpenEdit = (room) => {
        setSelectedRoom(room);
        setIsRoomEditModalOpen(true);
    };

    const handleOpenCreate = () => {
        setSelectedRoom(null);
        setIsRoomEditModalOpen(true);
    };

    const handleOpenCert = (room) => {
        setSelectedRoom(room);
        setIsCertModalOpen(true);
    };

    const getCategoryBadge = (category) => {
        const cat = category || 'RESIDENTIAL';
        const styles = {
            RESIDENTIAL: { background: '#f1f5f9', color: '#475569' },
            COMMERCIAL: { background: '#fef3c7', color: '#92400e' },
            OFFICE: { background: '#e0e7ff', color: '#3730a3' },
            SHOP: { background: '#dcfce7', color: '#166534' }
        };
        const s = styles[cat] || styles.RESIDENTIAL;
        return (
            <span style={{
                background: s.background,
                color: s.color,
                padding: '0.2rem 0.5rem',
                borderRadius: '4px',
                fontSize: '0.75rem',
                fontWeight: 600
            }}>
                {cat}
            </span>
        );
    };

    const columns = [
        { header: 'Unit No', accessor: 'roomNumber' },
        { header: 'Wing', accessor: 'wing' },
        { header: 'Floor', accessor: 'floor' },
        {
            header: 'Category',
            render: (row) => getCategoryBadge(row.unitCategory)
        },
        {
            header: 'NOC Charge',
            render: (row) => row.nocApplicable ? (
                <span style={{ color: 'var(--primary)', fontWeight: 600, fontSize: '0.85rem' }}>
                    ₹{Number(row.nocAmount || 0).toFixed(2)}/mo
                </span>
            ) : (
                <span style={{ color: 'var(--text-secondary-light)', fontSize: '0.8rem' }}>None</span>
            )
        },
        {
            header: 'Primary Occupant',
            render: (row) => row.resident ? `${row.resident.name} (${row.resident.email})` : 'Unassigned'
        },
        ...(isAdminOrMaster ? [{
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                    <Button
                        variant="secondary"
                        onClick={() => handleOpenOccupancy(row)}
                        title="Manage Occupants"
                        style={{ padding: '0.35rem 0.6rem', fontSize: '0.8rem' }}
                    >
                        <Users size={14} /> Occupants
                    </Button>
                    <Button
                        variant="secondary"
                        onClick={() => handleOpenCert(row)}
                        title="Share Certificate"
                        style={{ padding: '0.35rem 0.6rem', fontSize: '0.8rem' }}
                    >
                        <Award size={14} /> {row.shareCertificateNumber ? 'Certificate' : 'Share Cert'}
                    </Button>
                    <Button
                        variant="secondary"
                        onClick={() => handleOpenEdit(row)}
                        title="Edit Unit & NOC"
                        style={{ padding: '0.35rem 0.6rem', fontSize: '0.8rem' }}
                    >
                        <Edit2 size={14} />
                    </Button>
                </div>
            )
        }] : [])
    ];

    return (
        <div>
            <PageHeader
                title="Room Directory & Occupancy"
                breadcrumbs={['Admin', 'Rooms']}
                actionButton={
                    isAdminOrMaster && (
                        <Button onClick={handleOpenCreate}>
                            <Plus size={18} /> Add Unit
                        </Button>
                    )
                }
            />

            <DataTable
                columns={columns}
                data={rooms}
                isLoading={isLoading}
                emptyMessage="No rooms found."
            />

            {isAdminOrMaster && (
                <OccupancyModal
                    isOpen={isOccupancyModalOpen}
                    onClose={() => setIsOccupancyModalOpen(false)}
                    room={selectedRoom}
                    onOccupantsChanged={fetchData}
                />
            )}

            {isAdminOrMaster && (
                <RoomEditModal
                    isOpen={isRoomEditModalOpen}
                    onClose={() => setIsRoomEditModalOpen(false)}
                    room={selectedRoom}
                    onSaved={fetchData}
                />
            )}

            <ShareCertificateModal
                isOpen={isCertModalOpen}
                onClose={() => setIsCertModalOpen(false)}
                room={selectedRoom}
                onCertificateUpdated={fetchData}
            />
        </div>
    );
};

export default AdminRooms;
