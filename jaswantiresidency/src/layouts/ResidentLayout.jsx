import React from 'react';
import { LayoutDashboard, Receipt, MessageSquare, UserCircle } from 'lucide-react';
import PortalLayout from './PortalLayout';

const NAV_ITEMS = [
    { to: '/resident/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/resident/invoices', label: 'Bills & Invoices', icon: Receipt },
    { to: '/resident/complaints', label: 'Helpdesk', icon: MessageSquare },
    { to: '/resident/profile', label: 'Profile', icon: UserCircle },
];

const TITLES = {
    '/resident/dashboard': 'Dashboard',
    '/resident/invoices': 'Bills & Invoices',
    '/resident/complaints': 'Helpdesk',
    '/resident/profile': 'My Profile',
};

const ResidentLayout = () => (
    <PortalLayout portalName="Resident Portal" navItems={NAV_ITEMS} titles={TITLES} fallbackTitle="Resident Portal" />
);

export default ResidentLayout;
