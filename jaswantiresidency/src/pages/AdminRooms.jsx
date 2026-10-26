import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Award, Building2, Pencil, Plus, Users } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import StatusChip from '../components/ui/StatusChip';
import { Segmented } from '../components/ui/Feedback';
import { getRooms } from '../api/adminApi';
import OccupancyModal from '../components/OccupancyModal';
import RoomEditModal from '../components/RoomEditModal';
import ShareCertificateModal from '../components/ShareCertificateModal';
import { useAuth } from '../context/AuthContext';
import { notifyError } from '../utils/notify';
import { formatMoney } from '../utils/format';

const CATEGORY_LABELS = { RESIDENTIAL: 'Residential', COMMERCIAL: 'Commercial', OFFICE: 'Office', SHOP: 'Shop' };

const AdminRooms = () => {
    const { user } = useAuth();
    const isAdminOrMaster = user?.role === 'ADMIN' || user?.role === 'MASTER_ADMIN';

    const [rooms, setRooms] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [filter, setFilter] = useState('all');
    const [selectedRoom, setSelectedRoom] = useState(null);
    const [modal, setModal] = useState(null); // 'occupants' | 'edit' | 'certificate'

    const fetchData = useCallback(async () => {
        setIsLoading(true);
        try {
            setRooms((await getRooms()) || []);
        } catch (err) {
            notifyError('Could not load rooms. Please refresh the page.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { fetchData(); }, [fetchData]);

    const open = (kind, room = null) => { setSelectedRoom(room); setModal(kind); };
    const close = () => setModal(null);

    const counts = useMemo(() => ({
        all: rooms.length,
        occupied: rooms.filter((r) => r.resident).length,
        vacant: rooms.filter((r) => !r.resident).length,
        commercial: rooms.filter((r) => r.unitCategory && r.unitCategory !== 'RESIDENTIAL').length,
    }), [rooms]);

    const visible = useMemo(() => {
        if (filter === 'occupied') return rooms.filter((r) => r.resident);
        if (filter === 'vacant') return rooms.filter((r) => !r.resident);
        if (filter === 'commercial') return rooms.filter((r) => r.unitCategory && r.unitCategory !== 'RESIDENTIAL');
        return rooms;
    }, [rooms, filter]);

    const columns = [
        {
            header: 'Unit',
            accessor: 'roomNumber',
            render: (row) => (
                <div className="row" style={{ '--gap': '0.7rem' }}>
                    <span className="unit-badge">{row.roomNumber}</span>
                    <div className="cell-sub" style={{ marginTop: 0 }}>Wing {row.wing || '—'} · Floor {row.floor ?? '—'}</div>
                </div>
            ),
        },
        { header: 'Type', render: (row) => <span className="tag">{CATEGORY_LABELS[row.unitCategory] || 'Residential'}</span> },
        {
            header: 'Primary occupant',
            searchValue: (row) => `${row.resident?.name || ''} ${row.resident?.email || ''}`,
            render: (row) => (row.resident ? (
                <div>
                    <div className="cell-primary">{row.resident.name}</div>
                    <div className="cell-sub">{row.resident.email}</div>
                </div>
            ) : <StatusChip tone="neutral" label="Vacant" />),
        },
        {
            header: 'NOC charge',
            align: 'right',
            render: (row) => (row.nocApplicable ? <span className="fw-600">{formatMoney(row.nocAmount, 2)}<span className="text-subtle text-xs"> /mo</span></span> : <span className="text-subtle">—</span>),
        },
        ...(isAdminOrMaster ? [{
            header: <span className="visually-hidden">Actions</span>,
            render: (row) => (
                <div className="row-actions" onClick={(e) => e.stopPropagation()}>
                    <Button variant="secondary" size="sm" icon={Users} onClick={() => open('occupants', row)}>Occupants</Button>
                    <Button variant="ghost" size="sm" iconOnly icon={Award} onClick={() => open('certificate', row)}
                        aria-label={`Share certificate for ${row.roomNumber}`} title="Share certificate" />
                    <Button variant="ghost" size="sm" iconOnly icon={Pencil} onClick={() => open('edit', row)}
                        aria-label={`Edit unit ${row.roomNumber}`} title="Edit unit" />
                </div>
            ),
        }] : []),
    ];

    return (
        <div>
            <PageHeader
                title="Rooms & occupancy"
                description="Every unit in the building, who lives there, and any NOC charges billed alongside maintenance."
                actions={isAdminOrMaster && <Button icon={Plus} onClick={() => open('edit', null)}>Add unit</Button>}
            />

            <div className="row row-wrap row-between mb-4">
                <Segmented
                    label="Filter units"
                    value={filter}
                    onChange={setFilter}
                    options={[
                        { value: 'all', label: `All ${counts.all}` },
                        { value: 'occupied', label: `Occupied ${counts.occupied}` },
                        { value: 'vacant', label: `Vacant ${counts.vacant}` },
                        { value: 'commercial', label: `Commercial ${counts.commercial}` },
                    ]}
                />
            </div>

            <DataTable
                columns={columns}
                data={visible}
                isLoading={isLoading}
                searchPlaceholder="Search unit, name or email"
                emptyIcon={Building2}
                emptyTitle="No units here"
                emptyMessage={filter === 'all' ? 'Add the first unit to get started.' : 'No units match this filter.'}
                onRowClick={isAdminOrMaster ? (row) => open('occupants', row) : undefined}
            />

            {isAdminOrMaster && (
                <OccupancyModal isOpen={modal === 'occupants'} onClose={close} room={selectedRoom} onOccupantsChanged={fetchData} />
            )}
            {isAdminOrMaster && (
                <RoomEditModal isOpen={modal === 'edit'} onClose={close} room={selectedRoom} onSaved={fetchData} />
            )}
            <ShareCertificateModal isOpen={modal === 'certificate'} onClose={close} room={selectedRoom} onCertificateUpdated={fetchData} />
        </div>
    );
};

export default AdminRooms;
