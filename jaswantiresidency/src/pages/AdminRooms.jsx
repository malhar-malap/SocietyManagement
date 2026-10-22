import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import { Users } from 'lucide-react';
import { getRooms } from '../api/adminApi';
import OccupancyModal from '../components/OccupancyModal';
import { useAuth } from '../context/AuthContext';

const AdminRooms = () => {
    const { user } = useAuth();
    const isAdminOrMaster = user?.role === 'ADMIN' || user?.role === 'MASTER_ADMIN';

    const [rooms, setRooms] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const [selectedRoom, setSelectedRoom] = useState(null);
    const [isOccupancyModalOpen, setIsOccupancyModalOpen] = useState(false);

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

    const columns = [
        { header: 'Room Number', accessor: 'roomNumber' },
        { header: 'Wing', accessor: 'wing' },
        { header: 'Floor', accessor: 'floor' },
        {
            header: 'Primary Occupant',
            render: (row) => row.resident ? `${row.resident.name} (${row.resident.email})` : 'Unassigned'
        },
        ...(isAdminOrMaster ? [{
            header: 'Occupancy',
            render: (row) => (
                <Button
                    variant="secondary"
                    onClick={() => handleOpenOccupancy(row)}
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.4rem',
                        padding: '0.35rem 0.75rem',
                        fontSize: '0.85rem'
                    }}
                >
                    <Users size={15} /> Occupants
                </Button>
            )
        }] : [])
    ];

    return (
        <div>
            <PageHeader
                title="Room Directory & Occupancy"
                breadcrumbs={['Admin', 'Rooms']}
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
        </div>
    );
};

export default AdminRooms;
