import React from 'react';
import { LayoutDashboard, Building2, Users, Receipt, ArrowLeftRight, MessageSquare, Megaphone, ScrollText } from 'lucide-react';
import PortalLayout from './PortalLayout';

const NAV_GROUPS = [
    { items: [{ to: '/admin/dashboard', label: 'Overview', icon: LayoutDashboard }] },
    {
        label: 'Society',
        items: [
            { to: '/admin/rooms', label: 'Rooms', icon: Building2 },
            { to: '/admin/residents', label: 'Residents', icon: Users, roles: ['ADMIN'] },
            { to: '/admin/residents', label: 'Users & roles', icon: Users, roles: ['MASTER_ADMIN'] },
        ],
    },
    {
        label: 'Finance',
        items: [
            { to: '/admin/billing', label: 'Billing', icon: Receipt },
            { to: '/admin/transactions', label: 'Transactions', icon: ArrowLeftRight },
        ],
    },
    {
        label: 'Community',
        items: [
            { to: '/admin/complaints', label: 'Helpdesk', icon: MessageSquare },
            { to: '/admin/announcements', label: 'Announcements', icon: Megaphone },
        ],
    },
    { label: 'Administration', items: [{ to: '/admin/system-logs', label: 'System logs', icon: ScrollText, roles: ['MASTER_ADMIN'] }] },
];

const TITLES = {
    '/admin/dashboard': 'Overview',
    '/admin/rooms': 'Rooms & occupancy',
    '/admin/residents': (u) => (u?.role === 'MASTER_ADMIN' ? 'Users & roles' : 'Residents'),
    '/admin/users': (u) => (u?.role === 'MASTER_ADMIN' ? 'Users & roles' : 'Residents'),
    '/admin/billing': 'Billing',
    '/admin/transactions': 'Transactions',
    '/admin/complaints': 'Helpdesk',
    '/admin/announcements': 'Announcements',
    '/admin/system-logs': 'System logs',
};

const AdminLayout = () => (
    <PortalLayout portalName="Admin portal" navGroups={NAV_GROUPS} titles={TITLES} fallbackTitle="Admin portal" />
);

export default AdminLayout;
