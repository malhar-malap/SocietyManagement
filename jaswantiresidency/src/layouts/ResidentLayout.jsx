import React from 'react';
import { LayoutDashboard, Receipt, MessageSquare, UserCircle } from 'lucide-react';
import PortalLayout from './PortalLayout';

const NAV_GROUPS = [
    {
        items: [
            { to: '/resident/dashboard', label: 'Home', icon: LayoutDashboard },
            { to: '/resident/invoices', label: 'Bills & payments', icon: Receipt },
            { to: '/resident/complaints', label: 'Helpdesk', icon: MessageSquare },
            { to: '/resident/profile', label: 'My profile', icon: UserCircle },
        ],
    },
];

const TITLES = {
    '/resident/dashboard': 'Home',
    '/resident/invoices': 'Bills & payments',
    '/resident/complaints': 'Helpdesk',
    '/resident/profile': 'My profile',
};

const ResidentLayout = () => (
    <PortalLayout portalName="Resident portal" navGroups={NAV_GROUPS} titles={TITLES} fallbackTitle="Resident portal" profilePath="/resident/profile" />
);

export default ResidentLayout;
