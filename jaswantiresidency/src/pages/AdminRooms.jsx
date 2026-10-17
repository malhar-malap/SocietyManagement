import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import { Plus } from 'lucide-react';
import { getRooms, createRoom, getResidents } from '../api/adminApi';

const AdminRooms = () => {
    const [rooms, setRooms] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const fetchData = async () => {
        setIsLoading(true);
        try {
            const roomsData = await getRooms();
            setRooms(roomsData);
        } catch (err) {
            console.error("Failed to fetch data", err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);



    const columns = [
        { header: 'Room Number', accessor: 'roomNumber' },
        { header: 'Wing', accessor: 'wing' },
        { header: 'Floor', accessor: 'floor' },
        { header: 'Resident', render: (row) => row.resident ? `${row.resident.name} (${row.resident.email})` : 'Unassigned' }
    ];

    return (
        <div>
            <PageHeader 
                title="Room Directory" 
                breadcrumbs={['Admin', 'Rooms']} 
            />

            <DataTable 
                columns={columns} 
                data={rooms} 
                isLoading={isLoading}
                emptyMessage="No rooms found."
            />
        </div>
    );
};

export default AdminRooms;
