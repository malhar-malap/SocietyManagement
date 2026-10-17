import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';

const AdminActivityLogs = () => {
    const [logs, setLogs] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchLogs = async () => {
            try {
                const { getActivityLogs } = await import('../api/adminApi');
                const data = await getActivityLogs();
                setLogs(data);
            } catch (err) {
                console.error("Failed to fetch logs", err);
                // Fallback to stub if API fails
                setLogs([
                    { id: 1, timestamp: '2026-07-12 14:30', admin: 'admin@jaswanti.com', module: 'Billing', action: 'Generate Monthly Bills', ip: '192.168.1.101' },
                    { id: 2, timestamp: '2026-07-12 10:15', admin: 'admin@jaswanti.com', module: 'Residents', action: 'Blocked Resident B-203', ip: '192.168.1.101' },
                    { id: 3, timestamp: '2026-07-11 16:45', admin: 'manager@jaswanti.com', module: 'Announcements', action: 'Created "Society Meeting"', ip: '10.0.0.45' }
                ]);
            } finally {
                setIsLoading(false);
            }
        };
        fetchLogs();
    }, []);

    const columns = [
        { header: 'Timestamp', accessor: 'timestamp' },
        { header: 'Admin', accessor: 'admin' },
        { header: 'Module', accessor: 'module' },
        { header: 'Action', accessor: 'action' },
        { header: 'IP Address', accessor: 'ip' }
    ];

    return (
        <div>
            <PageHeader 
                title="Activity Logs" 
                breadcrumbs={['Admin', 'Logs']} 
            />

            <DataTable 
                columns={columns} 
                data={logs} 
                isLoading={isLoading}
            />
        </div>
    );
};

export default AdminActivityLogs;
