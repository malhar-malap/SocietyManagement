import React from 'react';
import './ui.css';

const StatusChip = ({ status }) => {
    const getStatusStyles = (status) => {
        const s = status?.toLowerCase() || '';
        switch (s) {
            case 'paid':
            case 'active':
            case 'resolved':
                return 'chip-success';
            case 'pending':
            case 'open':
            case 'in progress':
                return 'chip-warning';
            case 'blocked':
            case 'overdue':
                return 'chip-danger';
            case 'paid (cash)':
                return 'chip-info';
            default:
                return 'chip-default';
        }
    };

    return (
        <span className={`status-chip ${getStatusStyles(status)}`}>
            {status}
        </span>
    );
};

export default StatusChip;
